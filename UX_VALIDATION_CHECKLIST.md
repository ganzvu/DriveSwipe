# DriveSwipe UX Validation Checklist

## Acceptance Criteria Coverage
- [ ] Home starts/stops gesture control within 2 taps from launch.
- [ ] Setup wizard reaches "Drive Ready" only after all required permissions are granted.
- [ ] Users can remap core actions (next, previous, play/pause, volume up/down) without code changes.
- [ ] Advanced tuning is optional; defaults work for first-use driving scenarios.
- [ ] Home always shows active status and current mode.

## Reliability and Safety Checks
- [ ] Emergency Disable immediately stops foreground service.
- [ ] Cooldown value shown in UI matches runtime behavior in the service.
- [ ] Last recognized gesture and last action are visible on Home.
- [ ] History screen records recent recognized gestures and actions.
- [ ] Night mode behavior is clearly explained and switches correctly.

## In-Car Validation Pass
- [ ] First-run setup can be completed in under 90 seconds.
- [ ] No accidental trigger while hands remain on steering wheel for 5 minutes.
- [ ] False positive rate remains low in daytime and low-light conditions.
- [ ] One-handed operation is possible for emergency stop and quick start.
