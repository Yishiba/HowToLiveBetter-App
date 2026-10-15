#!/usr/bin/env python3
"""
App 图标与启动页图标的唯一来源：倾斜的天平（左盘空、高高翘起，是花掉的成本；右盘满、沉下去，是换回的收益）。

几何只在 GLYPH 里定义一次（100×100 坐标），由它生成：
  - Android 自适应图标：前景矢量（也作 Android 13 主题图标的单色层、Android 12 启动页图标）、背景色
  - Android 7 及以下用的 5 档 PNG（方形圆角、圆形各一套）
  - iOS 1024 图标、iOS 启动页用的白色天平
  - docs/brand/icon.svg（给人看、给商店用）

用法：python3 tools/icon/generate.py   （要 Pillow；改了 GLYPH 或颜色就重跑，产物入库）
"""
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
BRAND = "#2F7D57"   # 和 core/ui 的 primary 同色
WHITE = "#FFFFFF"

# ---------- 天平，100×100 坐标 ----------
# ("line", x1, y1, x2, y2, 线宽)：圆头线段
# ("dot", cx, cy, r)：实心圆
# ("bowl", x1, x2, y, 控制点 y, 线宽)：上沿是直线、下沿是二次曲线的实心碗（满满的收益盘）
GLYPH = [
    ("line", 50, 30, 50, 74, 6),      # 立柱
    ("line", 38, 76, 62, 76, 6),      # 底座
    ("line", 22, 36, 78, 50, 6),      # 横梁：右边沉
    ("dot", 50, 29, 5),               # 支点
    ("line", 22, 36, 22, 44, 4),      # 左吊绳
    ("line", 15, 44, 29, 44, 4),      # 左盘：空的，一条平线
    ("line", 78, 50, 78, 60, 4),      # 右吊绳
    ("bowl", 67, 89, 60, 72, 4),      # 右盘：满的
]
# 天平的外接框（含线宽），用来在不同画布里缩放
BBOX = (13, 24, 91, 79)
CENTER_X = 51


def bowl_points(x1, x2, y, cy, n=24):
    """碗的轮廓：上沿直线，下沿是 (x1,y)-(mid,cy)-(x2,y) 的二次贝塞尔"""
    mx = (x1 + x2) / 2
    pts = [(x1, y)]
    for i in range(1, n):
        t = i / n
        pts.append(((1 - t) ** 2 * x1 + 2 * (1 - t) * t * mx + t * t * x2,
                    (1 - t) ** 2 * y + 2 * (1 - t) * t * cy + t * t * y))
    pts.append((x2, y))
    return pts


def fit(box_size: float, glyph_width: float):
    """把天平放进边长 box_size 的画布正中，宽度占 glyph_width，返回 (缩放, x 偏移, y 偏移)"""
    x0, y0, x1, y1 = BBOX
    s = glyph_width / (x1 - x0)
    # 横向按立柱居中（稍向右让一点给更重的右盘），看起来才是一架立稳的天平；外接框居中会显得立柱偏左
    cx, cy = CENTER_X, (y0 + y1) / 2
    return s, box_size / 2 - cx * s, box_size / 2 - cy * s


# ---------- 位图：Pillow，8 倍超采样后缩小，边缘够平滑 ----------
SS = 8


def draw_glyph(d: ImageDraw.ImageDraw, size: int, glyph_width: float, color: str):
    s, tx, ty = fit(size, glyph_width)
    P = lambda x, y: (x * s + tx, y * s + ty)
    for el in GLYPH:
        kind = el[0]
        if kind == "line":
            _, x1, y1, x2, y2, w = el
            a, b, r = P(x1, y1), P(x2, y2), w * s / 2
            d.line([a, b], fill=color, width=max(1, round(w * s)))
            for cx, cy in (a, b):
                d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
        elif kind == "dot":
            _, cx, cy, r = el
            c = P(cx, cy)
            d.ellipse([c[0] - r * s, c[1] - r * s, c[0] + r * s, c[1] + r * s], fill=color)
        elif kind == "bowl":
            _, x1, x2, y, cy, w = el
            pts = [P(*p) for p in bowl_points(x1, x2, y, cy)]
            d.polygon(pts, fill=color)
            # 描边让碗沿圆润，和 SVG 的 stroke-linejoin=round 一致
            r = w * s / 2
            d.line(pts + [pts[0]], fill=color, width=max(1, round(w * s)))
            for px, py in pts:
                d.ellipse([px - r, py - r, px + r, py + r], fill=color)


def render(size: int, shape: str, glyph_width: float, bg: str | None = BRAND, fg: str = WHITE) -> Image.Image:
    """shape：square（满版方形）、rounded（圆角方形）、circle（圆形）、none（透明底，只画天平）"""
    big = size * SS
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if shape == "square":
        d.rectangle([0, 0, big, big], fill=bg)
    elif shape == "rounded":
        d.rounded_rectangle([0, 0, big - 1, big - 1], radius=big * 0.23, fill=bg)
    elif shape == "circle":
        d.ellipse([0, 0, big - 1, big - 1], fill=bg)
    draw_glyph(d, big, glyph_width * SS, fg)
    return img.resize((size, size), Image.LANCZOS)


# ---------- 矢量：Android VectorDrawable 与 SVG ----------
def bowl_path(x1, x2, y, cy):
    mx = (x1 + x2) / 2
    return f"M{x1},{y} Q{mx},{cy} {x2},{y} Z"


def vector_drawable(glyph_width_dp: float) -> str:
    """108dp 自适应图标画布的前景，天平居中，宽度 glyph_width_dp（安全区是中间直径 66dp 的圆）"""
    s, tx, ty = fit(108, glyph_width_dp)
    items = []
    for el in GLYPH:
        kind = el[0]
        if kind == "line":
            _, x1, y1, x2, y2, w = el
            items.append(f'        <path android:pathData="M{x1},{y1} L{x2},{y2}" android:strokeColor="#FFFFFF" '
                         f'android:strokeWidth="{w}" android:strokeLineCap="round" />')
        elif kind == "dot":
            _, cx, cy, r = el
            items.append(f'        <path android:pathData="M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z" '
                         f'android:fillColor="#FFFFFF" />')
        elif kind == "bowl":
            _, x1, x2, y, cy, w = el
            items.append(f'        <path android:pathData="{bowl_path(x1, x2, y, cy)}" android:fillColor="#FFFFFF" '
                         f'android:strokeColor="#FFFFFF" android:strokeWidth="{w}" android:strokeLineJoin="round" />')
    body = "\n".join(items)
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- 由 tools/icon/generate.py 生成，别手改。倾斜的天平：左盘空（成本），右盘满（收益）。 -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="{s:.4f}"
        android:scaleY="{s:.4f}"
        android:translateX="{tx:.4f}"
        android:translateY="{ty:.4f}">
{body}
    </group>
</vector>
'''


def svg(size=100, glyph_width=78) -> str:
    s, tx, ty = fit(size, glyph_width)
    items = []
    for el in GLYPH:
        kind = el[0]
        if kind == "line":
            _, x1, y1, x2, y2, w = el
            items.append(f'<path d="M{x1} {y1}L{x2} {y2}" stroke="{WHITE}" stroke-width="{w}" stroke-linecap="round"/>')
        elif kind == "dot":
            _, cx, cy, r = el
            items.append(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{WHITE}"/>')
        elif kind == "bowl":
            _, x1, x2, y, cy, w = el
            items.append(f'<path d="{bowl_path(x1, x2, y, cy)}" fill="{WHITE}" stroke="{WHITE}" stroke-width="{w}" stroke-linejoin="round"/>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}">\n'
            f'<!-- 由 tools/icon/generate.py 生成。倾斜的天平：左盘空（成本），右盘满（收益）。 -->\n'
            f'<rect width="{size}" height="{size}" rx="{size * 0.23:.1f}" fill="{BRAND}"/>\n'
            f'<g transform="translate({tx:.3f} {ty:.3f}) scale({s:.4f})">\n' + "\n".join(items) + "\n</g>\n</svg>\n")


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, Image.Image):
        data.save(path, optimize=True)
    else:
        path.write_text(data, encoding="utf-8")
    print("  ", path.relative_to(ROOT))


def main():
    res = ROOT / "androidApp/src/main/res"
    print("Android 自适应图标与启动页")
    # 前景宽 58dp：落在直径 66dp 的安全区里，各品牌裁成圆、方、水滴都不会切到
    write(res / "drawable/ic_launcher_foreground.xml", vector_drawable(58))
    write(res / "values/ic_launcher_background.xml",
          f'<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/icon/generate.py 生成 -->\n<resources>\n'
          f'    <color name="ic_launcher_background">{BRAND}</color>\n</resources>\n')
    for name in ("ic_launcher", "ic_launcher_round"):
        write(res / f"mipmap-anydpi-v26/{name}.xml",
              '<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/icon/generate.py 生成 -->\n'
              '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
              '    <background android:drawable="@color/ic_launcher_background" />\n'
              '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
              '    <monochrome android:drawable="@drawable/ic_launcher_foreground" />\n'
              '</adaptive-icon>\n')

    print("Android 7 及以下的位图图标")
    for dpi, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        write(res / f"mipmap-{dpi}/ic_launcher.png", render(px, "rounded", px * 0.72))
        write(res / f"mipmap-{dpi}/ic_launcher_round.png", render(px, "circle", px * 0.62))

    print("iOS")
    assets = ROOT / "iosApp/iosApp/Assets.xcassets"
    # App Store 要求图标不透明、方形满版，圆角由系统加
    write(assets / "AppIcon.appiconset/AppIcon-1024.png", render(1024, "square", 1024 * 0.66).convert("RGB"))
    write(assets / "AppIcon.appiconset/Contents.json",
          '{\n  "images" : [\n    {\n      "filename" : "AppIcon-1024.png",\n      "idiom" : "universal",\n'
          '      "platform" : "ios",\n      "size" : "1024x1024"\n    }\n  ],\n'
          '  "info" : {\n    "author" : "xcode",\n    "version" : 1\n  }\n}\n')
    # 启动页：品牌绿底，中间白色天平
    # 140pt：和 Android 12 系统启动页里天平的视觉大小相当
    for scale in (1, 2, 3):
        write(assets / f"LaunchIcon.imageset/LaunchIcon@{scale}x.png", render(140 * scale, "none", 140 * scale * 0.9))
    write(assets / "LaunchIcon.imageset/Contents.json",
          '{\n  "images" : [\n' + ",\n".join(
              f'    {{ "filename" : "LaunchIcon@{s}x.png", "idiom" : "universal", "scale" : "{s}x" }}' for s in (1, 2, 3))
          + '\n  ],\n  "info" : { "author" : "xcode", "version" : 1 }\n}\n')
    r, g, b = (int(BRAND[i:i + 2], 16) / 255 for i in (1, 3, 5))
    write(assets / "LaunchBackground.colorset/Contents.json",
          '{\n  "colors" : [\n    {\n      "color" : {\n        "color-space" : "srgb",\n        "components" : {\n'
          f'          "alpha" : "1.000",\n          "blue" : "{b:.3f}",\n          "green" : "{g:.3f}",\n          "red" : "{r:.3f}"\n'
          '        }\n      },\n      "idiom" : "universal"\n    }\n  ],\n  "info" : { "author" : "xcode", "version" : 1 }\n}\n')

    print("源文件与商店图")
    write(ROOT / "docs/brand/icon.svg", svg())
    write(ROOT / "docs/brand/icon-512.png", render(512, "square", 512 * 0.66).convert("RGB"))


if __name__ == "__main__":
    main()
