#!/bin/bash




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

urxvtd -q -o -f

exec i3

#urxvtd -q -o -f
