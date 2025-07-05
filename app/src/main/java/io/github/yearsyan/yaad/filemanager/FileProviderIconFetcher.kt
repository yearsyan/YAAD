package io.github.yearsyan.yaad.filemanager

import android.content.Context
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult


class FileProviderIconFetcher(private val context: Context, private val provider: IFileNodeProvider): Fetcher  {
    override suspend fun fetch(): FetchResult? {
        if (provider.iconType == IconType.FETCHER) {
            val bitmap = provider.fetchIcon()
            if (bitmap == null) {
                return null
            }
            return ImageFetchResult(
                image = bitmap.toDrawable(context.resources).asImage(),
                isSampled = false,
                dataSource = DataSource.MEMORY,
            )
        }
        return null
    }

    class Factory : Fetcher.Factory<IFileNodeProvider> {
        override fun create(
            data: IFileNodeProvider,
            options: coil3.request.Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return FileProviderIconFetcher(options.context, data)
        }
    }
}