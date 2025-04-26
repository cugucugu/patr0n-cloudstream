version = 1

cloudstream {
    authors     = listOf("patr0n")
    language    = "tr"
    description = "Dizifun."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=www.dizifun2.com&sz=%size%"
}