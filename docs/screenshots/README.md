# DisasterMesh Screenshots

This directory contains app screenshots for the README and documentation.

## Required Screenshots

### Main Interface (3 images)
- `main-interface-light.png` - Main app in light mode
- `main-interface-dark.png` - Main app in dark mode  
- `emergency-map.png` - Map view with location and alerts

### SOS Flow (4 images)
- `sos-button.png` - Red SOS emergency button
- `sos-confirmation.png` - SOS confirmation dialog
- `sos-sent.png` - SOS alert sent notification
- `sos-received.png` - SOS alert received on another device

### Mesh Status (3 images)
- `mesh-connecting.png` - Mesh network connecting state
- `mesh-connected.png` - Active mesh connections
- `mesh-debug.png` - Debug information dialog

### Admin Features (4 images)
- `admin-login.png` - Admin authentication dialog
- `admin-broadcast.png` - Admin broadcast creation
- `risk-levels.png` - Risk level selection (Low/Medium/High/Critical)
- `broadcast-sent.png` - Broadcast alert sent confirmation

### Network Visualization (2 images)
- `network-topology.png` - Mesh network topology diagram
- `alert-propagation.png` - Alert propagation visualization

## Capture Instructions

1. **Setup**: Install app on 2+ Android devices
2. **Pair**: Connect devices via Bluetooth settings
3. **Test**: Open app and test all features
4. **Capture**: Use `adb shell screencap -p /sdcard/screenshot.png`
5. **Transfer**: `adb pull /sdcard/screenshot.png`

## Image Specifications

- **Format**: PNG (for screenshots), GIF (for demos)
- **Size**: Mobile screenshots should be 250-400px wide
- **Quality**: High resolution, clear text
- **Naming**: Use descriptive kebab-case names