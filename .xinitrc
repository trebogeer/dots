#!/bin/bash

# X session startup, sourced by xinit(1)/LightDM.

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
# urxvtd
#
# The daemon owns the control socket at
# $HOME/.urxvt/urxvtd-<nodename> and spawns urxvt clients.
#
#   -q  quiet
#   -o  open a connection to $DISPLAY and keep it open
#   -f  fork into the background once the socket is bound
#
# A stale socket left over from a previous session makes urxvtd
# fail to bind, so it is removed first when no daemon is
# listening on it any more.
# ------------------------------------------------------------

mkdir -p "$HOME/.urxvt"
chmod 700 "$HOME/.urxvt"

if ! pgrep -u "$(id -u)" -x urxvtd >/dev/null 2>&1; then
    rm -f "$HOME/.urxvt/urxvtd-"*
    urxvtd -q -o -f
fi

# ------------------------------------------------------------
# Screen lock
# ------------------------------------------------------------

xautolock -time 5 -locker "i3lock -i $HOME/.i3/ak47-181456.png -t" &

exec i3
