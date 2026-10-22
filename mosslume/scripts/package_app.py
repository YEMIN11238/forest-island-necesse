#!/usr/bin/env python3
"""Package Mosslume with the existing source-built native runtime and island adapter."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import plistlib
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT.parent / 'necesse-island'
ICON_DIR = ROOT / 'branding/app-icon'
SOURCE_ICON = Path('/Users/zhangjunhao/Downloads/dbb8c4169775beba6f44880b2bd11311.jpg')


def run(*args: str | Path) -> None:
    subprocess.run([str(arg) for arg in args], check=True)


def copy_atomic(source: str | Path, target: str | Path) -> str:
    """Replace the inode instead of rewriting cached/signed Mach-O files in place."""
    target=Path(target)
    fd,temporary=tempfile.mkstemp(prefix='.'+target.name+'.',dir=target.parent)
    os.close(fd)
    try:
        shutil.copy2(source,temporary)
        os.replace(temporary,target)
    finally:
        if os.path.exists(temporary):os.unlink(temporary)
    return str(target)


def prepare_icon() -> Path:
    ICON_DIR.mkdir(parents=True, exist_ok=True)
    original = ICON_DIR / 'source-backpack.jpg'
    if not original.exists():
        shutil.copy2(SOURCE_ICON, original)
    iconset = ICON_DIR / 'Mosslume.iconset'
    iconset.mkdir(exist_ok=True)
    for logical in (16, 32, 128, 256, 512):
        for retina in (1, 2):
            actual = logical * retina
            suffix = '@2x' if retina == 2 else ''
            target = iconset / f'icon_{logical}x{logical}{suffix}.png'
            run('/usr/bin/sips', '-s', 'format', 'png', '-z', str(actual), str(actual), original, '--out', target)
    icon = ICON_DIR / 'Mosslume.icns'
    run('/usr/bin/iconutil', '-c', 'icns', iconset, '-o', icon)
    (ICON_DIR / 'provenance.json').write_text(json.dumps({
        'source': str(SOURCE_ICON), 'savedOriginal': str(original),
        'sourceSha256': hashlib.sha256(original.read_bytes()).hexdigest(),
        'policy': 'Use exact user-supplied square graphic; technical resize/PNG/ICNS conversion only. No crop, background removal or redesign.',
        'iconset': str(iconset), 'icns': str(icon),
    }, ensure_ascii=False, indent=2) + '\n')
    return icon


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--icon-only', action='store_true', help='Prepare icon files without packaging or launching.')
    parser.add_argument('--compile-launcher-only', action='store_true', help='Compile native universal launcher without copying runtime.')
    parser.add_argument('--skip-icon', action='store_true', help='Use previously prepared ICNS.')
    parser.add_argument('--app-path', type=Path, default=Path.home()/'Applications/Mosslume.app', help='Package a separate preview without replacing the stable application.')
    parser.add_argument('--profile-name', default='Mosslume', help='Isolated default data directory for this application.')
    parser.add_argument('--version', default='0.3.0', help='Bundle version label.')
    parser.add_argument('--skip-links', action='store_true', help='Do not alter project or desktop daily-use entries.')
    parser.add_argument('--no-profile-migration', action='store_true', help='Start an isolated new-world profile without importing legacy saves.')
    options = parser.parse_args()
    if '/' in options.profile_name or options.profile_name in ('', '.', '..'):
        parser.error('--profile-name must be a directory name')
    icon = ICON_DIR / 'Mosslume.icns' if options.skip_icon else prepare_icon()
    if options.icon_only:
        print(icon); return
    launcher = ROOT / 'build/MosslumeLauncher'
    launcher.parent.mkdir(parents=True, exist_ok=True)
    run('/usr/bin/clang', '-O2', '-arch', 'arm64', '-arch', 'x86_64', '-framework', 'AppKit',
        '-mmacosx-version-min=11.0', ROOT / 'scripts/native_launcher.m', '-o', launcher)
    launcher.chmod(0o755)
    if options.compile_launcher_only:
        print(launcher); return
    runtime = BASE / 'runtime/Necesse.app/Contents/Resources'
    kernel = runtime / 'Necesse-source-built-1.3.3.jar'
    mod = BASE / 'mod/dist/localforest-island.jar'
    for path in (runtime / 'jre/bin/java', kernel, mod, icon):
        if not path.is_file():
            raise SystemExit(f'Missing required input: {path}')
    installed = options.app_path.expanduser().resolve()
    contents = installed / 'Contents'
    mac = contents / 'MacOS'
    res = contents / 'Resources'
    mac.mkdir(parents=True, exist_ok=True)
    res.mkdir(parents=True, exist_ok=True)
    game = res / 'Game'
    game.mkdir(exist_ok=True)
    # Copy dependencies into the app, never symlink through a protected Documents folder.
    # Preserve license notices. Exclude source build alternatives, server kernel and old logs.
    selected = ['jre', 'lib', 'locale', 'controller_config', 'res.data',
                'Necesse-source-built-1.3.3.jar', 'steam_appid.txt', 'Licenses.txt']
    for name in selected:
        source = runtime / name
        if not source.exists():
            continue
        target = game / name
        if source.is_dir():
            shutil.copytree(source, target, dirs_exist_ok=True, copy_function=copy_atomic)
        else:
            copy_atomic(source, target)
    # The loader reads these loose overrides before mod classes initialize.
    branding = BASE / 'mod/resources/mosslume/branding'
    loose_resources = game / 'res'
    overrides = {'title.png': 'keyart.png', 'blank-logo.png': 'logo.png'}
    installed_overrides = []
    for source_name, target_name in overrides.items():
        source = branding / source_name
        if source.exists():
            loose_resources.mkdir(exist_ok=True)
            copy_atomic(source, loose_resources / target_name)
            installed_overrides.append('res/' + target_name)
    mods = res / 'Mods'
    mods.mkdir(exist_ok=True)
    copy_atomic(mod, mods / 'localforest-island.jar')
    copy_atomic(launcher, mac / 'MosslumeLauncher')
    copy_atomic(icon, res / 'Mosslume.icns')
    info = {
        'CFBundleName': 'Mosslume', 'CFBundleDisplayName': 'Mosslume',
        'CFBundleIdentifier': 'local.mosslume.game.preview' if options.skip_links else 'local.mosslume.game',
        'CFBundleExecutable': 'MosslumeLauncher',
        'CFBundlePackageType': 'APPL', 'CFBundleShortVersionString': options.version,
        'CFBundleVersion': '4', 'CFBundleIconFile': 'Mosslume.icns',
        'MosslumeProfileName': options.profile_name,
        'MosslumeMigrateLegacyProfile': not options.no_profile_migration,
        'NSHighResolutionCapable': True, 'LSMinimumSystemVersion': '11.0',
        'NSHumanReadableCopyright': 'Mosslume development build',
    }
    (contents / 'Info.plist').write_bytes(plistlib.dumps(info))
    run('/usr/bin/codesign', '--force', '--sign', '-', installed)
    run('/usr/bin/codesign', '--verify', installed)
    project_app = ROOT / 'Mosslume.app'
    if not options.skip_links and not project_app.exists() and not project_app.is_symlink():
        project_app.symlink_to(installed, target_is_directory=True)
    elif not options.skip_links and project_app.is_symlink() and project_app.resolve() != installed:
        project_app.unlink(); project_app.symlink_to(installed, target_is_directory=True)
    desktop = Path.home() / 'Desktop/Mosslume.app'
    if not options.skip_links and not desktop.exists() and not desktop.is_symlink():
        desktop.symlink_to(installed, target_is_directory=True)
    evidence = ROOT / ('build/package-preview-manifest.json' if options.skip_links else 'build/package-manifest.json')
    evidence.write_text(json.dumps({
        'app': str(installed), 'projectEntry': str(project_app), 'desktopEntry': str(desktop),
        'icon': str(res / 'Mosslume.icns'), 'profile': str(Path.home() / 'Library/Application Support'/options.profile_name),
        'version': options.version, 'stableEntriesUnchanged': options.skip_links,
        'sourceBuiltKernelSha256': hashlib.sha256(kernel.read_bytes()).hexdigest(),
        'adapterSha256': hashlib.sha256(mod.read_bytes()).hexdigest(),
        'iconSha256': hashlib.sha256(icon.read_bytes()).hexdigest(),
        'launcherArchitectures': subprocess.check_output(['/usr/bin/lipo', '-archs', str(launcher)], text=True).strip(),
        'defaultLaunch': 'branded menu, -islandprofile -mosslume; no automatic -load',
        'earlyLoadingArtOverrides': installed_overrides,
        'profileMigration': 'Disabled; isolated new-world profile.' if options.no_profile_migration else 'At first normal launch, copy old saves only if the new saves directory is absent; copy settings.cfg only if absent. Old profile untouched.',
        'runtimeSelfContained': True, 'launched': False,
    }, ensure_ascii=False, indent=2) + '\n')
    print(installed)


if __name__ == '__main__':
    main()
