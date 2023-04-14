package cardMastery.patches;

import basemod.ReflectionHacks;
import basemod.patches.com.megacrit.cardcrawl.cards.AbstractCard.RenderFixSwitches;
import cardMastery.CardMastery;
import cardMastery.helper.Mastery;
import cardMastery.helper.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class MasteryBGPatches {
    //catch the render call for the bg if found and replace that with my own if needed
    @SpirePatch(clz = RenderFixSwitches.RenderBgSwitch.class, method = "Prefix")
    public static class RenderBgForModdedCards {
        @SpireInsertPatch(locator = Locator.class, localvars = {"region"})
        public static void patch(AbstractCard __instance, SpriteBatch sb, float xPos, float yPos, Color ___renderColor, @ByRef TextureAtlas.AtlasRegion[] region) {
            if(CardMastery.shouldBG() && Mastery.shouldShowMastery(__instance)) {
                String tex = getBgString(__instance, true);
                if(tex != null) {
                    region[0] = TextureLoader.getTextureAsAtlasRegion(tex);
                }
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                //Don't even ask me why, this should be renderHelper
                Matcher finalMatcher = new Matcher.MethodCallMatcher(RenderFixSwitches.class, "access$000");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch2(clz = AbstractCard.class, method = "renderAttackBg")
    @SpirePatch2(clz = AbstractCard.class, method = "renderSkillBg")
    @SpirePatch2(clz = AbstractCard.class, method = "renderPowerBg")
    public static class RenderBgForBaseGameCard {
        static ReflectionHacks.RMethod meth = ReflectionHacks.privateMethod(AbstractCard.class, "renderHelper", SpriteBatch.class, Color.class, TextureAtlas.AtlasRegion.class, float.class, float.class);
        @SpirePrefixPatch
        public static SpireReturn<?> patch(AbstractCard __instance, SpriteBatch sb, float x, float y) {
            if(CardMastery.shouldBG() && Mastery.shouldShowMastery(__instance)) {
                String tex = getBgString(__instance, false);
                if (tex != null) {
                    meth.invoke(__instance,
                            sb,
                            ReflectionHacks.getPrivate(__instance, AbstractCard.class, "renderColor"),
                            TextureLoader.getTextureAsAtlasRegion(tex),
                            x, y
                    );
                    return SpireReturn.Return();
                }
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch2(clz = SingleCardViewPopup.class, method = "renderCardBack")
    public static class OverrideCardbackForSCV {
        @SpireInsertPatch(locator = AtlasLocator.class, localvars = {"tmpImg"})
        public static void atlasPatch(@ByRef TextureAtlas.AtlasRegion[] tmpImg, AbstractCard ___card) {
            if(CardMastery.shouldBG() && Mastery.shouldShowMastery(___card)) {
                String tex = getBgString(___card, true);
                if(tex != null) {
                    tmpImg[0] = TextureLoader.getTextureAsAtlasRegion(tex);
                }
            }
        }

        @SpireInsertPatch(locator = TextureLocator.class, localvars = {"img"})
        public static void texturePatch(@ByRef Texture[] img, AbstractCard ___card) {
            if(CardMastery.shouldBG() && Mastery.shouldShowMastery(___card)) {
                String tex = getBgString(___card, true);
                if(tex != null) {
                    img[0] = TextureLoader.getTexture(tex);
                }
            }
        }

        private static class AtlasLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(SingleCardViewPopup.class, "renderHelper");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }

        private static class TextureLocator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(SpriteBatch.class, "draw");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    public static String getBgString(AbstractCard c, boolean bigBG) {
        String dir = bigBG? "big" : "small";
        switch (c.type) {
            case POWER:
            case ATTACK:
            case SKILL:
                return CardMastery.makeImagePath("/card/"+dir+"/" + c.type.name().toLowerCase() + ".png");
            case CURSE:
                if (CardMastery.shouldSkipCurses())
                    return null;
            default:
                return CardMastery.makeImagePath("/card/"+dir+"/skill.png");
        }
    }
}