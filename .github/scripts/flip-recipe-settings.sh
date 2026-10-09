#!/bin/sh
# Turns every recipe setting in the given MineFantasy config directory away from its default, so a second game test
# run loads the recipe code the default settings skip.
c="$1"
sed -i -e 's/\(B:"Allow Iron ingots to make Pig Iron"\)=false/\1=true/' \
  -e 's/\(B:"Cobblestone Hammering"\)=true/\1=false/' \
  -e 's/\(B:"Enable Transformations"\)=true/\1=false/' \
  -e 's/\(B:"Log Chopping"\)=true/\1=false/' \
  -e 's/\(B:"Plank Sawing"\)=true/\1=false/' \
  -e 's/\(B:"Refined Plank Sawing"\)=true/\1=false/' "$c/Crafting.cfg"
sed -i -e 's/\(B:"Allow Stone-Age"\)=true/\1=false/' \
  -e 's/\(B:"Hardcore Ingots"\)=true/\1=false/' \
  -e 's/\(B:"Remove Recipes"\)=true/\1=false/' \
  -e 's/\(B:"Remove Books Recipes"\)=false/\1=true/' \
  -e 's/\(B:"Remove Talismans Recipes"\)=false/\1=true/' "$c/Hardcore.cfg"
sed -i -e 's/\(B:"Enable Kitchen Bench"\)=true/\1=false/' "$c/Kitchen.cfg"
