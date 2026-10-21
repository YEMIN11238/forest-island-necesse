from pathlib import Path
import plistlib,shutil,subprocess,json
root=Path(__file__).resolve().parents[1]
app=root.parent/'林间小岛·Necesse.app'
mac=app/'Contents/MacOS';res=app/'Contents/Resources'
mac.mkdir(parents=True,exist_ok=True);res.mkdir(parents=True,exist_ok=True)
# Older development-only launcher linked to a protected Documents folder.
project=res/'Project'
if project.is_symlink():project.unlink()
shutil.copytree(root/'runtime/Necesse.app/Contents/Resources',res/'Game',dirs_exist_ok=True)
(res/'Mods').mkdir(exist_ok=True)
shutil.copy2(root/'mod/dist/localforest-island.jar',res/'Mods/localforest-island.jar')
subprocess.run(['/usr/bin/clang','-O2','-arch','arm64','-arch','x86_64',str(root/'scripts/native_launcher.c'),'-o',str(mac/'IslandLauncher')],check=True)
(mac/'IslandLauncher').chmod(0o755)
icon=res/'Game/icons.icns'
if icon.exists():shutil.copy2(icon,res/'App.icns')
info={'CFBundleName':'林间小岛·Necesse','CFBundleDisplayName':'林间小岛·Necesse','CFBundleIdentifier':'local.forest.necesseisland.native','CFBundleExecutable':'IslandLauncher','CFBundlePackageType':'APPL','CFBundleVersion':'2','CFBundleShortVersionString':'0.1.0','CFBundleIconFile':'App','NSHighResolutionCapable':True,'LSMinimumSystemVersion':'11.0'}
(app/'Contents/Info.plist').write_bytes(plistlib.dumps(info))
installed=Path.home()/'Applications/林间小岛·Necesse.app';installed.parent.mkdir(exist_ok=True)
shutil.copytree(app,installed,dirs_exist_ok=True)
subprocess.run(['/usr/bin/codesign','--force','--sign','-',str(installed)],check=True)
desktop=Path.home()/'Desktop/林间小岛·Necesse.app'
if desktop.is_symlink() and desktop.resolve()==app:desktop.unlink()
if not desktop.exists() and not desktop.is_symlink():desktop.symlink_to(installed,target_is_directory=True)
(root/'evidence/app-package.json').write_text(json.dumps({'app':str(installed),'desktop':str(desktop),'selfContainedRuntime':True,'nativeLauncher':True,'sourceBuiltKernel':'Necesse-source-built-1.3.3.jar','dataDirectory':str(Path.home()/'Library/Application Support/ForestIslandNecesse')},ensure_ascii=False,indent=2))
print(installed)
