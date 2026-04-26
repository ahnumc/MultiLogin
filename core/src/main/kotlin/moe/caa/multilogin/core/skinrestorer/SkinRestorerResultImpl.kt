package moe.caa.multilogin.core.skinrestorer

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.skinrestorer.SkinRestorerResult
import moe.caa.multilogin.api.profile.GameProfile

/**
 * 皮肤修复结果
 */
class SkinRestorerResultImpl private constructor(
    override val reason: SkinRestorerResult.Reason,
    override val response: GameProfile?,
    override val throwable: Throwable?
) : SkinRestorerResult {

    companion object {
        fun ofNoSkin(): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.NO_SKIN, null, null)

        fun ofNoRestorer(): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.NO_RESTORER, null, null)

        fun ofSignatureValid(): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.SIGNATURE_VALID, null, null)

        fun ofRestorerAsync(): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.RESTORER_ASYNC, null, null)

        fun ofUseCache(profile: GameProfile): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.USE_CACHE, profile, null)

        fun ofRestorerSucceed(profile: GameProfile): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.RESTORER_SUCCEED, profile, null)

        fun ofBadSkin(throwable: Throwable): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.BAD_SKIN, null, throwable)

        fun ofRestorerFailed(throwable: Throwable): SkinRestorerResultImpl =
            SkinRestorerResultImpl(SkinRestorerResult.Reason.RESTORER_FAILED, null, throwable)

        fun handleSkinRestoreResult(throwable: Throwable?) {
            LoggerProvider.logger.error("An exception occurred while processing the skin repair.", throwable)
        }

        fun handleSkinRestoreResult(result: SkinRestorerResultImpl) {
            result.throwable?.let(::handleSkinRestoreResult)
        }
    }
}
