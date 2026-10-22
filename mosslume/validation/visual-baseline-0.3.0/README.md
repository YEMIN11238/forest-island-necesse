# 重构前视觉基线

当前基线为 0.3.0 夜景调整版本，适配器 SHA-256 为 `720feaac7f568127f946797281d1e88659591444937e753e7848205a2afa1234`。

- `s2-night-tuning/full-frame.png`：原生游戏 21 点场景。
- `s2-night-tuning/comparison-hour.png`：同一视角 12 点场景。
- `s2-night-tuning/preview.mp4`：按真实采样时间编码的原生录制，前半夜间、后半白天，无补帧。
- `s2-night-tuning/performance.json`：独立于截图录制的 6 秒帧间隔记录。
- `night-tuning/verification.json`：当前调整与实际检查范围。
- `evidence-s2/verification.json`：之前 S2 的历史验证及当前夜景更新索引。其旧哈希和旧性能记录不能代替当前版本。

重构后保留像素清晰度、树冠风动、局部暖光、物品和海岸投影。当前记录不表示完整玩法验收、稳定 60 fps 或已接入 AI。历史记录中的绝对路径保留为原始来源定位。
