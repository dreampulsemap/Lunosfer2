import os
import xml.etree.ElementTree as ET

# Define English strings
strings_en = {
    "dream_addTitle": "Log a Dream",
    "dream_placeholder": "What did you dream about? Describe the sights, sounds, and feelings...",
    "dream_location": "Location",
    "dream_public": "Public",
    "dream_friends": "Friends only",
    "dream_private": "Private",
    "dream_shareInFeed": "Share in public feed",
    "dream_submit": "Submit Dream",
    "dream_validationContent": "Please describe your dream before submitting.",
    "dream_createFailed": "Failed to log dream. Please try again.",
    "dream_unknownLocation": "Unknown Location",
    # 16 emotions
    "dream_emotion_joy": "😊 Joy",
    "dream_emotion_sadness": "😢 Sadness",
    "dream_emotion_fear": "😨 Fear",
    "dream_emotion_anger": "😡 Anger",
    "dream_emotion_surprise": "😲 Surprise",
    "dream_emotion_disgust": "🤢 Disgust",
    "dream_emotion_trust": "🤝 Trust",
    "dream_emotion_anticipation": "🤩 Anticipation",
    "dream_emotion_love": "❤️ Love",
    "dream_emotion_anxiety": "😰 Anxiety",
    "dream_emotion_confusion": "😕 Confusion",
    "dream_emotion_awe": "🌌 Awe",
    "dream_emotion_peace": "🕊️ Peace",
    "dream_emotion_nostalgia": "🕰️ Nostalgia",
    "dream_emotion_guilt": "😔 Guilt",
    "dream_emotion_hope": "✨ Hope"
}

res_dir = "app/src/main/res"

for folder in os.listdir(res_dir):
    if folder.startswith("values"):
        xml_file = os.path.join(res_dir, folder, "strings.xml")
        if os.path.exists(xml_file):
            tree = ET.parse(xml_file)
            root = tree.getroot()
            
            # Remove existing dream_ to prevent duplicates if we run this twice
            for child in list(root):
                if child.attrib.get('name', '').startswith('dream_'):
                    root.remove(child)
                    
            for key, val in strings_en.items():
                el = ET.SubElement(root, "string", name=key)
                el.text = val
                
            tree.write(xml_file, encoding="utf-8", xml_declaration=True)

