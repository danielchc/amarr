package amarr.indexer.filters

class MediaFilter {

    companion object {
        // Source
        // https://github.com/Radarr/Radarr/blob/develop/src/NzbDrone.Core/MediaFiles/MediaFileExtensions.cs
        // https://github.com/Radarr/Radarr/blob/develop/src/NzbDrone.Core/MediaFiles/FileExtensions.cs
        private val mediaExtensions=listOf(".webm",".m4v",".3gp",".nsv",".ty",".strm",".rm",".rmvb",".m3u",".ifo",".mov",".qt",".divx",".xvid",".bivx",".nrg",".pva",".wmv",".asf",".asx",".ogm",".ogv",".m2v",".avi",".bin",".dat",".dvr-ms",".mpg",".mpeg",".mp4",".avc",".vp3",".svq3",".nuv",".viv",".dv",".fli",".flv",".wpl",".img",".iso",".vob",".mkv",".mk3d",".ts",".wtv",".m2ts",".7z",".bz2",".gz",".r00",".rar",".tar.bz2",".tar.gz",".tar",".tb2",".tbz2",".tgz",".zip",".zipx")

        fun mediaFilter(fileName:String):Boolean{
            var validFile=false;

            for (extension in mediaExtensions){
                if(fileName.endsWith(extension)){
                    validFile=true;
                    break
                }
            }

            return validFile
        }
    }


}