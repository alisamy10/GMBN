package com.endeavour.gmbn.repository


import androidx.lifecycle.LiveData
import com.endeavour.gmbn.api.YoutubeService
import com.endeavour.gmbn.db.VideoDao
import com.endeavour.gmbn.vm.CommentResponse
import com.endeavour.gmbn.vm.Resource
import com.endeavour.gmbn.vm.Video
import com.endeavour.gmbn.vm.VideoDetailedResponse

class GMBNRepository private constructor(
    private val youtubeService: YoutubeService,
    private val videoDao: VideoDao
) {

    suspend fun getVideoIds() : Resource<List<String>>{
        return try {
            val videos = youtubeService.videoIds(KEY, CHANNEL_ID, SNIPPET, ORDER, MAX_RESULTS)
            if(videos.isSuccessful){
                Resource.success(videos.body()?.items?.map { it.toId()} ?: emptyList())
            }else {
                Resource.error(videos.message(),emptyList())
            }
        }catch (ex: Exception){
            Resource.connection(emptyList())
        }

    }

    fun searchVideos(ids: List<String>) : LiveData<Resource<List<Video>>> {
        return object : NetworkBoundResource<List<Video>, VideoDetailedResponse>() {
            override suspend fun saveCallResult(item: VideoDetailedResponse) = videoDao.insertAll(item.items.map { it.toEntity() })

            override fun loadFromDb() = videoDao.getVideos()

            override suspend fun createCall() = youtubeService.detailedVideos(KEY, ids.joinToString(), FULL_DETAILS, ORDER)

        }.asLiveData()
    }

    fun loadVideoById(videoId: String) = videoDao.loadVideoById(videoId)

     fun loadCachedVideos() = videoDao.loadCachedVideos()

    suspend fun loadVideoComments(videoId: String): Resource<CommentResponse> {

        return try {
            val comments = youtubeService.videoComments(KEY, videoId, SNIPPET)
            if(comments.isSuccessful){
                Resource.success(comments.body())
            }else {
                Resource.error(comments.message(),CommentResponse(emptyList()))
            }
        }catch (ex: Exception){
            Resource.connection(CommentResponse(emptyList()))
        }
    }

    companion object {
        //TODO:: Add your own API KEY
        // https://developers.google.com/youtube/v3/getting-started
        val KEY = ""
        val CHANNEL_ID = "UC_A--fhX5gea0i4UtpD99Gg"
        val SNIPPET = "snippet"
        val FULL_DETAILS = "snippet,contentDetails,statistics"
        val ORDER = "date"
        val MAX_RESULTS = 50

        @Volatile
        private var instance: GMBNRepository? = null

        fun getInstance(youtubeService: YoutubeService, videoDao: VideoDao) =
            instance ?: synchronized(this) {
                instance ?: GMBNRepository(youtubeService, videoDao).also { instance = it }
            }
    }
}