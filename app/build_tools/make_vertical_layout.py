'''
Script to automatically generate a vertical layout from
the landscape layout of the player activity
'''

import xml.etree.ElementTree
import os

this_dir = os.path.dirname(os.path.realpath(__file__))

tree = xml.etree.ElementTree.parse(this_dir + "/../src/main/res/layout-land/activity_player.xml")

linear_layout = tree.getroot()

# Convert the main linear layout to vertical
for attrib in linear_layout.attrib:
    if "orientation" in attrib:
        linear_layout.attrib[attrib] = "vertical"
        break
        
# Swap the 2 child elements
linear_layout[0],linear_layout[1] = linear_layout[1], linear_layout[0] 
for child in linear_layout:
    for attrib in child.attrib:
        if "height" in attrib:
            child.attrib[attrib] = "0dp"
        elif "width" in attrib:
            child.attrib[attrib] = "match_parent"

# Add comments
comment = xml.etree.ElementTree.Comment("AUTOMATICALLY GENERATED FROM LANDSCAPE LAYOUT, DO NOT EDIT")
linear_layout.insert(0, comment)

tree.write(this_dir + "/../src/main/res/layout/activity_player.xml", xml_declaration=True, encoding="UTF-8")