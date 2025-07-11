package com.viditnakhawa.photowatermarkcard.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

object ExifUtils {
    fun copyExifData(context: Context, sourceUri: Uri, destUri: Uri) {
        try {
            // Use the source URI to get an InputStream for the original file's EXIF data
            context.contentResolver.openInputStream(sourceUri)?.use { sourceStream ->
                val sourceExif = ExifInterface(sourceStream)

                // Use the destination URI to get a FileDescriptor to write EXIF data to the new file
                context.contentResolver.openFileDescriptor(destUri, "rw")?.use { destFd ->
                    val destExif = ExifInterface(destFd.fileDescriptor)

                    // An extensive list of all supported EXIF tags to copy
                    val tags = arrayOf(
                        ExifInterface.TAG_APERTURE_VALUE, ExifInterface.TAG_ARTIST,
                        ExifInterface.TAG_BITS_PER_SAMPLE, ExifInterface.TAG_COMPRESSION,
                        ExifInterface.TAG_CONTRAST, ExifInterface.TAG_COPYRIGHT,
                        ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_DIGITIZED,
                        ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
                        ExifInterface.TAG_DIGITAL_ZOOM_RATIO, ExifInterface.TAG_EXIF_VERSION,
                        ExifInterface.TAG_EXPOSURE_BIAS_VALUE, ExifInterface.TAG_EXPOSURE_INDEX,
                        ExifInterface.TAG_EXPOSURE_MODE, ExifInterface.TAG_EXPOSURE_PROGRAM,
                        ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_FILE_SOURCE,
                        ExifInterface.TAG_FLASH, ExifInterface.TAG_FLASH_ENERGY,
                        ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
                        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, ExifInterface.TAG_F_NUMBER,
                        ExifInterface.TAG_GAIN_CONTROL, ExifInterface.TAG_GPS_ALTITUDE,
                        ExifInterface.TAG_GPS_ALTITUDE_REF, ExifInterface.TAG_GPS_AREA_INFORMATION,
                        ExifInterface.TAG_GPS_DATESTAMP, ExifInterface.TAG_GPS_DEST_BEARING,
                        ExifInterface.TAG_GPS_DEST_BEARING_REF, ExifInterface.TAG_GPS_DEST_DISTANCE,
                        ExifInterface.TAG_GPS_DEST_DISTANCE_REF, ExifInterface.TAG_GPS_DEST_LATITUDE,
                        ExifInterface.TAG_GPS_DEST_LATITUDE_REF, ExifInterface.TAG_GPS_DEST_LONGITUDE,
                        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, ExifInterface.TAG_GPS_DIFFERENTIAL,
                        ExifInterface.TAG_GPS_DOP, ExifInterface.TAG_GPS_IMG_DIRECTION,
                        ExifInterface.TAG_GPS_IMG_DIRECTION_REF, ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_MAP_DATUM,
                        ExifInterface.TAG_GPS_MEASURE_MODE, ExifInterface.TAG_GPS_PROCESSING_METHOD,
                        ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_SPEED,
                        ExifInterface.TAG_GPS_SPEED_REF, ExifInterface.TAG_GPS_STATUS,
                        ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_TRACK,
                        ExifInterface.TAG_GPS_TRACK_REF, ExifInterface.TAG_GPS_VERSION_ID,
                        ExifInterface.TAG_IMAGE_DESCRIPTION, ExifInterface.TAG_IMAGE_LENGTH,
                        ExifInterface.TAG_IMAGE_UNIQUE_ID, ExifInterface.TAG_IMAGE_WIDTH,
                        ExifInterface.TAG_INTEROPERABILITY_INDEX, ExifInterface.TAG_ISO_SPEED_RATINGS,
                        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT, ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                        ExifInterface.TAG_LIGHT_SOURCE, ExifInterface.TAG_MAKE,
                        ExifInterface.TAG_MAKER_NOTE, ExifInterface.TAG_MAX_APERTURE_VALUE,
                        ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_MODEL,
                        ExifInterface.TAG_OECF, ExifInterface.TAG_ORIENTATION,
                        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, ExifInterface.TAG_PIXEL_X_DIMENSION,
                        ExifInterface.TAG_PIXEL_Y_DIMENSION, ExifInterface.TAG_PLANAR_CONFIGURATION,
                        ExifInterface.TAG_PRIMARY_CHROMATICITIES, ExifInterface.TAG_REFERENCE_BLACK_WHITE,
                        ExifInterface.TAG_RESOLUTION_UNIT, ExifInterface.TAG_ROWS_PER_STRIP,
                        ExifInterface.TAG_SAMPLES_PER_PIXEL, ExifInterface.TAG_SATURATION,
                        ExifInterface.TAG_SCENE_CAPTURE_TYPE, ExifInterface.TAG_SCENE_TYPE,
                        ExifInterface.TAG_SENSING_METHOD, ExifInterface.TAG_SHARPNESS,
                        ExifInterface.TAG_SHUTTER_SPEED_VALUE, ExifInterface.TAG_SOFTWARE,
                        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE, ExifInterface.TAG_SPECTRAL_SENSITIVITY,
                        ExifInterface.TAG_STRIP_BYTE_COUNTS, ExifInterface.TAG_STRIP_OFFSETS,
                        ExifInterface.TAG_SUBJECT_AREA, ExifInterface.TAG_SUBJECT_DISTANCE,
                        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE, ExifInterface.TAG_SUBJECT_LOCATION,
                        ExifInterface.TAG_SUBSEC_TIME, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
                        ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH, ExifInterface.TAG_TRANSFER_FUNCTION,
                        ExifInterface.TAG_USER_COMMENT, ExifInterface.TAG_WHITE_BALANCE,
                        ExifInterface.TAG_WHITE_POINT, ExifInterface.TAG_X_RESOLUTION,
                        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS, ExifInterface.TAG_Y_CB_CR_POSITIONING,
                        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING, ExifInterface.TAG_Y_RESOLUTION
                    )

                    // Loop through all tags, get the attribute from the source and set it on the destination
                    for (tag in tags) {
                        val value = sourceExif.getAttribute(tag)
                        if (value != null) {
                            destExif.setAttribute(tag, value)
                        }
                    }
                    destExif.saveAttributes() // This saves the changes to the destination file
                    println("Successfully copied EXIF data.")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("Could not copy EXIF data: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("An unexpected error occurred during EXIF data copy: ${e.message}")
        }
    }
}