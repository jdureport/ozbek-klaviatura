package uz.yangialifbo.klaviatura;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Emoji bölimlari va oxirgi işlatilgan emojilar. */
final class Emoji {

    private Emoji() {}

    static final int PER_PAGE = 32;

    /** Bölim belgisi (pastki qatorda) — 0-bölim: oxirgilar. */
    static final String[] ICONS = {"🕘", "😀", "👍", "❤️", "🐱", "🍎", "⚽"};

    private static final String[] CATEGORIES = {
            "",
            "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 😉 😊 😇 🥰 😍 🤩 😘 😋 😜 🤔 🤗 🤭 😐 😏 😒 🙄 😬 😌 😔 😴 😎 🥳 😢",
            "👍 👎 👌 ✌️ 🤞 🤟 🤘 👏 🙌 👐 🤲 🙏 🤝 💪 👋 🤚 ✋ 🖐️ 👆 👇 👈 👉 ☝️ ✍️ 💅 🤳 👀 🧠 👶 👦 👧 🧕",
            "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 ✨ ⭐ 🌟 💫 🔥 💯 ✅ ❌ ❗ ❓ ⚡ 🎉 🎊 💥",
            "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐑 🐴 🐫 🐔 🐧 🐦 🦄 🐝 🦋 🐢 🐬 🌸 🌹 🌻 🌷 🌳 🍀 🍁 ☀️ 🌙",
            "🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍒 🍑 🍈 🍍 🥝 🍅 🥕 🌽 🥒 🧅 🧄 🥔 🍞 🫓 🧀 🥚 🍳 🍗 🍖 🍕 🍔 🍰 🍵 🫖",
            "⚽ 🏀 🏐 🎾 🏆 🥇 🎮 🎵 🎤 🎧 📚 ✏️ 📌 💡 📱 💻 ⌚ 📷 🎁 🚗 🚌 ✈️ 🚀 🏠 🏫 🕌 ⏰ 📅 💰 🇺🇿 🌍 🎓",
    };

    static List<String> category(Context c, int index) {
        if (index == 0) return recent(c);
        return Arrays.asList(CATEGORIES[index].split(" "));
    }

    static List<String> recent(Context c) {
        String s = Prefs.get(c).getString("emoji_oxirgi", "");
        List<String> out = new ArrayList<>();
        for (String e : s.split("\n")) if (!e.isEmpty()) out.add(e);
        return out;
    }

    static void addRecent(Context c, String emoji) {
        List<String> list = recent(c);
        list.remove(emoji);
        list.add(0, emoji);
        while (list.size() > PER_PAGE) list.remove(list.size() - 1);
        Prefs.get(c).edit().putString("emoji_oxirgi", String.join("\n", list)).apply();
    }
}
