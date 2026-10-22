# dots

Personal desktop setup for a minimal Ubuntu install: Xorg + i3,
urxvt (managed through `urxvtd`), i3status, dmenu and vim.

## Layout

| Repo path | Installed to | Purpose |
| --- | --- | --- |
| `.i3/config` | `~/.config/i3/config` | i3 window manager |
| `.i3/i3status.conf` | `~/.i3/i3status.conf` | bar modules |
| `.i3/scripts/` | `~/.i3/scripts/` | helper scripts |
| `.i3/ak47-181456.png` | `~/.i3/` | wallpaper and lock screen |
| `.Xdefaults` | `~/.Xdefaults` | urxvt resources (font, colors, transparency, geometry) |
| `.xinitrc` | `~/.xinitrc` | X session startup |
| `.vimrc` | `~/.vimrc` | vim + Vundle |
| `.config/systemd/user/urxvtd.service` | same | urxvt daemon |
| `.config/environment.d/90-ssh-agent.conf` | same | `SSH_AUTH_SOCK` for the session |

Scripts:

* `bootstrap_new` – installs every package (see below).
* `machine_setup` – adapts the configuration to the current machine.
* `monitors` – watches i3's `output` events and re-applies the monitor
  layout when a monitor is plugged in or unplugged.
* `i3stat` – the bar's `status_command`; wallpaper and `i3status`.
* `dblmon` – two-monitor layout (called by `monitors`).
* `laptop` – internal panel only (called by `monitors`).
* `kbdswitch` – keyboard layout switching.
* `bootstrap` – the original package list; superseded by `bootstrap_new`.

## Install

Run everything from the console, not over SSH: the network setup
is replaced half-way through.

```sh
git clone git@github.com:trebogeer/dots.git ~/dots

sudo ~/dots/.i3/scripts/bootstrap_new

cp -r ~/dots/.i3 ~/dots/.config ~/dots/.Xdefaults ~/dots/.xinitrc ~/

sudo ~/.i3/scripts/machine_setup
```

Then reboot (`graphical.target` starts LightDM) or run `startx`.

The files are copied, not symlinked on purpose: `machine_setup`
rewrites the machine-specific values in `~/.i3/i3status.conf` and
`~/.config/i3/config`, and the repo should stay generic.

### What `bootstrap_new` does

Installs, in this order:

* Xorg and xinit
* i3, i3status, i3lock
* rxvt-unicode, plus a systemd user unit for `urxvtd`
  (the daemon is started by the unit, not by `~/.xinitrc`)
* utilities: vim, git, xsel, xclip, xautolock, feh, curl, wget,
  ca-certificates, tmux, htop
* `x11-xserver-utils` (xrdb, xset, xrandr) and `x11-xkb-utils`
  (setxkbmap)
* dmenu from `suckless-tools` (Ubuntu 26.04 no longer ships a
  standalone `dmenu` package)
* build toolchain for YouCompleteMe (build-essential, cmake,
  pkg-config)
* NetworkManager and nm-applet
* LightDM, enabled as the display manager
* Google Chrome (downloaded from Google)
* Vundle, and it warns that `:PluginInstall` is still needed for
  the vim plugins; YouCompleteMe is compiled at that point
* `graphical.target` as the default target

At the end it verifies that all required commands exist.

`bootstrap_new` **installs packages only**. It does not create or
modify `~/.i3`, `~/.config/i3`, `~/.Xdefaults` or `~/.xinitrc`
(with the exception of the urxvtd unit, which it copies from the
repo if present).

### What `machine_setup` does

`bootstrap_new` installs the same packages everywhere, but a
desktop and a laptop differ:

* wired interface name (`eno1`, `enp0s31f6`, ...)
* Wi-Fi and battery presence
* whether NetworkManager or systemd-networkd should own the NIC

`machine_setup` detects the hardware from `/sys/class/net` (a
physical interface has a `device` link and type 1; loopback,
bridges, bonds, veth and docker0 are skipped) and from
`/sys/class/power_supply`, then rewrites:

* `~/.i3/i3status.conf`
  * `order += "ethernet <name>"` and the `ethernet <name> {` block
  * enables the `wireless` module when a Wi-Fi interface exists
  * enables the `battery` module when a battery exists
* `~/.config/i3/config`
  * `exec --no-startup-id nm-applet` and `tray_output primary`
    when NetworkManager is used, both commented out otherwise
* `/etc/netplan/99-networkmanager.yaml`
  * `renderer: NetworkManager` when NetworkManager is used, so
    that nm-applet actually sees the interfaces (otherwise NM
    manages nothing and the tray icon says "no connections")

NetworkManager is used where Wi-Fi exists (that is where it
matters), and a wired-only desktop stays on systemd-networkd.
Options:

```
sudo ~/.i3/scripts/machine_setup [--dry-run]
                                 [--network-manager|--no-network-manager]
                                 [--no-restart]
```

* `--dry-run` – print the actions, change nothing
* `--network-manager` – use NM even without Wi-Fi
* `--no-network-manager` – never use NM (removes the drop-in)
* `--no-restart` – do not restart i3 after changing files

The script is idempotent and restarts i3 when the files changed.

## Machine notes

This desktop: single wired `eno1`, no Wi-Fi, no battery, so
systemd-networkd is in charge, the bar shows `E: <ip> (<speed>)`
and there is no applet. A laptop gets the wireless and battery
modules, NetworkManager and the applet in the tray.

`~/.xinitrc` marks the internal panel (or the first connected
output) as primary, because `tray_output primary` needs one and X
does not mark any output by default.

## Monitor hotplug

`~/.i3/scripts/monitors` is started by i3 itself:

```
exec_always --no-startup-id ~/.i3/scripts/monitors
```

i3 already listens to the X RandR events and re-broadcasts them as
its `output` IPC event, so the script subscribes with
`i3-msg -t subscribe -m '["output"]'`. Each event triggers a
re-read of the connected outputs and, when the set changed, runs
`laptop` (one output) or `dblmon` (two or more). The comparison
keeps i3's duplicate events and the xrandr calls made by the
script itself from looping.

`exec_always` runs on startup and on `i3-msg restart` (a reload
does not run it). The subscription ends when i3 restarts; the new
i3 starts a new instance, and a lock file keeps the dying and the
new instance from overlapping.

## Notes

* i3status reads its configuration once at startup. After editing
  `~/.i3/i3status.conf`, restart i3 (`Mod+Shift+r` or
  `i3-msg restart`); `i3-msg reload` is not enough.
* urxvt sizes its first window from `URxvt*geometry` and then
  keeps the cursor that distance from the bottom of the window.
  The oversized `400x200` geometry makes the first resize shrink,
  which keeps the shell prompt in the top-left corner.
* `~/.Xdefaults` is only applied because `~/.xinitrc` runs
  `xrdb -merge`.
* Keyboard: us/ru, toggled with Alt+Shift.
* The urxvt daemon is owned by `urxvtd.service`; do not start
  `urxvtd` from `~/.xinitrc` as well.

## Troubleshooting

* Bar shows no ethernet address: run `machine_setup` (it rewrites
  the interface name), then `i3-msg restart`.
  `ip -o link show` tells the real name.
* No applet icon: the machine has no Wi-Fi, or no primary output
  is marked (`xrandr --output <name> --primary`).
* urxvt prompt appears in the middle of the window: check that
  `xrdb -query | grep geometry` returns `URxvt.geometry: 400x200`.
* Monitor plugged in but the layout is not applied: check that
  the watcher runs (`pgrep -af .i3/scripts/monitors`), or apply
  it by hand (`~/.i3/scripts/dblmon`).
