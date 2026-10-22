#!/bin/bash

# X session startup, sourced by xinit(1)/LightDM.

# ------------------------------------------------------------
# SSH agent
#
# ssh-agent.socket (systemd user unit) provides the socket and
# sets SSH_AUTH_SOCK in the user manager environment.  Export
# it here so the X session inherits it.  The key is added to the
# agent by the agent service itself.
# ------------------------------------------------------------

export SSH_AUTH_SOCK="/run/user/$(id -u)/openssh_agent"

# ------------------------------------------------------------
# X resources
#
# ~/.Xdefaults holds the urxvt resources (font, transparency,
# colors, perl extensions).  They are only applied if they are
# loaded into the X server, so this has to happen before urxvt
# starts.  "xrdb" comes from the x11-xserver-utils package.
# ------------------------------------------------------------

if [ -f "$HOME/.Xdefaults" ]; then
    xrdb -merge "$HOME/.Xdefaults"
fi

# ------------------------------------------------------------
# Keyboard
# ------------------------------------------------------------

setxkbmap -layout us,ru
#dvorak
setxkbmap -option
setxkbmap -option 'grp:alt_shift_toggle,grp_led:scroll'
#setxkbmap -variant ',,dvorak'
#setxkbmap -option 'grp:shift_toggle'
#setxkbmap -option 'grp_led:caps'
#setxkbmap -option 'ctrl:swapcaps'
#setxkbmap -option 'ctrl:nocaps'
xset r rate 260

# ------------------------------------------------------------
# Primary output
#
# i3bar docks the tray (tray_output primary) on the output that
# is marked primary, and X does not mark one by itself.  Prefer
# the internal panel, fall back to the first connected output.
# ------------------------------------------------------------

PRIMARY=$(xrandr -q | awk '
    / connected/ {
        if ($1 ~ /^(eDP|LVDS)/) { print $1; found = 1; exit }
        if (first == "") first = $1
    }
    END { if (!found && first != "") print first }')

if [ -n "$PRIMARY" ]; then
    xrandr --output "$PRIMARY" --primary --auto
fi

# ------------------------------------------------------------
# urxvtd
#
# The daemon owns the control socket at
# $HOME/.urxvt/urxvtd-<nodename> and spawns urxvt clients.
#
# It is started by the systemd user unit
# ~/.config/systemd/user/urxvtd.service, NOT from here.  Both
# starting it would leave two daemons fighting over the same
# socket.  The unit creates $HOME/.urxvt and clears a stale
# socket before binding.
# ------------------------------------------------------------

mkdir -p "$HOME/.urxvt"
chmod 700 "$HOME/.urxvt"

# ------------------------------------------------------------
# Screen lock
# ------------------------------------------------------------

xautolock -time 5 -locker "i3lock -i $HOME/.i3/ak47-181456.png -t" &

exec i3
