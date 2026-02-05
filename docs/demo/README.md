# DisasterMesh Demo Assets

This directory contains demo GIFs and videos for the README and documentation.

## Required Demo Content

### Main Demo GIF
- `disaster-mesh-demo.gif` - 30-second overview showing:
  1. App launch and permissions
  2. Device pairing via Bluetooth
  3. SOS button press and confirmation
  4. Alert received on second device
  5. Mesh status showing connection

### Feature-Specific Demos
- `sos-flow-demo.gif` - Complete SOS emergency flow
- `admin-broadcast-demo.gif` - Admin creating and sending broadcast
- `mesh-connection-demo.gif` - Devices discovering and connecting
- `offline-maps-demo.gif` - Map functionality without internet

## Recording Instructions

### Tools
- **Android**: Use `scrcpy` for screen mirroring + OBS/QuickTime for recording
- **Command**: `scrcpy --max-size 1024 --bit-rate 8M`

### Recording Guidelines
1. **Duration**: Keep GIFs under 30 seconds for fast loading
2. **Quality**: Balance file size vs clarity (aim for <5MB per GIF)
3. **Flow**: Show complete user workflows, not just individual features
4. **Speed**: Normal speed for important actions, 1.5x for navigation
5. **Text**: Ensure UI text is readable in the recording

### Conversion to GIF
```bash
# Using ffmpeg to convert video to optimized GIF
ffmpeg -i input.mp4 -vf "fps=10,scale=600:-1:flags=lanczos,palettegen" palette.png
ffmpeg -i input.mp4 -i palette.png -filter_complex "fps=10,scale=600:-1:flags=lanczos[x];[x][1:v]paletteuse" output.gif
```

## Demo Script

### 30-Second Overview Demo
1. **0-5s**: App launch, show main interface with map
2. **5-10s**: Tap mesh status, show "Connecting..." then "Connected: 1 device"
3. **10-15s**: Press red SOS button, show confirmation dialog
4. **15-20s**: Confirm SOS, show "Alert Sent" notification
5. **20-25s**: Switch to second device, show "Emergency Alert Received"
6. **25-30s**: Show alert details with location and timestamp

This creates a compelling 30-second story that demonstrates the core value proposition.