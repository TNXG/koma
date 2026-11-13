package moe.tnxg.koma.platform.photo

import android.net.Uri

/** Implemented by the resumed Android Activity that owns the photo-picker contract. */
interface PhotoPickerHost {
    /** Opens the system picker and suspends until a photo is chosen or the sheet is dismissed. */
    suspend fun pickPhotoUri(): Uri?
}
