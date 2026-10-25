# Input Optimizer: Minecraft 1.21.11, version 1.0.6

This build fixes Windows RAWKEYBOARD field offsets, preserves press/release/repeat
and modifier state, and reads signed vertical/horizontal wheel data correctly.
The current WM_INPUT packet is read before the remaining buffered packets. Buffer
traversal uses each packet's declared size and alignment. Failed native startup
falls back to ordinary callbacks. Windows supported raw keys/buttons use a single
input source so their normal callbacks cannot deliver a second copy.

Relative mouse movement shares the arrival-ordered keyboard/button/scroll queue.
Only contiguous movement is combined. It is applied through Minecraft's normal
sensitivity/inversion/smooth-camera math before the next discrete event; the hit
result is refreshed from the current aim before that event. Trailing movement is
applied immediately at the end of the available queue. No timer, sorting, extra
frame buffer, or network flushing is introduced. Inventory cursor motion stays
on the normal cursor path.

## Controls

Open Mods > Input Optimizer > Configure, or the client command `/inputoptimizer`.
Five Yes/No controls are saved in `config/inputoptimizer.json`:

- Optimization: Yes by default; No selects normal Minecraft input on restart.
- Raw keyboard: Yes by default; restart required.
- Raw mouse: Yes by default; restart required.
- Immediate keybinds: Yes by default; restart required.
- Latency profiler: No by default; can be changed live.

Restart-only settings are snapshots for the current session. Saving a toggle does
not silently switch or unregister input sources during gameplay. Chat and command
opening retain the 1.0.5 fix; held attack/use stay on vanilla ticks.

## Latency profiler

Use `/inputlatency on`, `/inputlatency`, `/inputlatency reset`,
`/inputlatency save`, and `/inputlatency off`, or the Latency results screen.
Save report writes a timestamped text file in the game's `inputoptimizer-reports`
directory, including the active mode and FPS when saved. Sampling is bounded to
4096 recent observations per metric, with no per-event file writes or sorting.
Reports show average, p50, p95, p99 and maximum times in milliseconds:

- Callback handling: accepted key/button callback entry to callback return.
- Callback -> keybind: a fresh attack/use/hotbar press to click consumption.
- Callback -> action start: a matched press to the client attack/use attempt or
  corresponding hotbar selection. This does not mean the server accepted it.
- Raw read -> replay: raw packet read to replay; optimized raw mode only.

Comparison: enable profiling in both runs, save a report, toggle Optimization,
restart Minecraft, reset samples, repeat the same scene/input pattern/FPS cap,
and save the second report. Compare matching callback/keybind/action metrics.
Raw and normal input have different upstream delivery paths; these internal
metrics cannot prove total latency improvement or physical-click-to-screen timing.
The OS time before packet reading, hardware polling, display latency and network
latency are excluded. Menu/focus changes clear pending correlations; stale and
unmatched inputs are not counted as action-start samples.

## Validation

Build with Java 21+:

```powershell
.\gradlew.bat -p inputoptimizer-1.21.11 build --offline --no-daemon
```

Production checks cover input ordering and duplicate counts, chat timing, native
Windows layouts, movement/click ordering, disabled-source/mode behavior, and
bounded/matched/expired timing samples. These checks do not launch Dawn or validate
live mouse feel, UI rendering, F3/chords, server behavior or hardware latency.
After restarting Dawn, check F3 on/off and chords, slash/T chat opening, inventory
click/drag, left/right modifiers and side buttons, scrolling, focus loss, sensitivity,
and move -> click -> move away aiming. Test profiling with both optimization modes.
