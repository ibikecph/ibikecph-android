#!/bin/bash

for file in res/drawable-xxhdpi/*.png; do newname=`echo $(basename "$file") | sed -e 's/\-/\_/g' | sed -E -e 's/[a-z]([A-Z])/\_\1/g' | tr "[A-Z]" "[a-z]"`; mv $file $(dirname "$file")/$newname; done
for file in res/drawable-mdpi/*.png; do newname=`echo $(basename "$file") | sed -e 's/\-/\_/g' | sed -E -e 's/[a-z]([A-Z])/\_\1/g' | tr "[A-Z]" "[a-z]"`; mv $file $(dirname "$file")/$newname; done
for file in res/drawable-hdpi/*.png; do newname=`echo $(basename "$file") | sed -e 's/\-/\_/g' | sed -E -e 's/[a-z]([A-Z])/\_\1/g' | tr "[A-Z]" "[a-z]"`; mv $file $(dirname "$file")/$newname; done
for file in res/drawable-xhdpi/*.png; do newname=`echo $(basename "$file") | sed -e 's/\-/\_/g' | sed -E -e 's/[a-z]([A-Z])/\_\1/g' | tr "[A-Z]" "[a-z]"`; mv $file $(dirname "$file")/$newname; done
