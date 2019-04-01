# Node-RED Flows for Hubitat Elevation

This directory contains various flows for monitoring, managing, and maybe mangling a Hubitat Elevation hub.
***
No warranty is offered, neither expressed nor implied.  Use of the flows contained herein is at the user's own risk.  Hazards of running these flows include, but are not limited to: unpredictable results, destruction of data, hub freezes, and the selling of your first-born on the black market.

| File | Description |
| ------ | ------ |
| HubitatBackups.json            | Periodically download a copy of the latest hub backup for safe-keeping |
| HubitatPerformanceMonitor.json | Monitors a Hubitat Elevation hub's response time via requests to the App List page |
| HubitatWebsocketRelay.json     | Proxy the websockets from the hub to a pair of local endpoints to reduce potential hub load |
