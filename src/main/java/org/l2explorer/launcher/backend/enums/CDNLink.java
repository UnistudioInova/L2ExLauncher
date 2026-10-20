package org.l2explorer.launcher.backend.enums;

public enum CDNLink
{
    // The two trailing params are the NC "Purple" update-protocol coordinates (TCP:27500) used by -last_version:
    // the service id (baked into the CDN path) and the update host. A null update host = that region's update
    // server is not known yet (its version cannot be live-resolved; see NcUpdateVersionClient / the version brief).
    // 07/26/2024 - latest is 529
    NC_SOFT_TAIWAN("http://mmorepo.cdn.plaync.com.tw/TWLin2EP20/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWLin2EP20/%d/Patch/PatchFileInfo_TWLin2EP20_%d.dat", "TWLin2EP20", "up4svr.plaync.com.tw"),
    NC_SOFT_KOREAN("http://l2kor.ncupdate.com/%s", "http://l2kor.ncupdate.com/L2_KOR/%d/Patch/files_info.json.zip", "L2_KOR", "up4svr.ncupdate.com"),
    NC_SOFT_AION_KOREAN("http://aionkor.ncupdate.com/%s", "http://aionkor.ncupdate.com/AION_KOR/%d/Patch/files_info.json.zip", "AION_KOR", "up4svr.ncupdate.com"),
    NC_SOFT_BNS_NEO_KOREA("http://bnsneo.ncupdate.com/%s", "http://bnsneo.ncupdate.com/BNSNEO_LIVE/%d/Patch/files_info.json.zip", "BNSNEO_LIVE", "up4svr.ncupdate.com"),
    NC_SOFT_BNS_TEST_KOREA("http://bnskortest.ncupdate.com/%s", "http://bnskortest.ncupdate.com/BNS_TEST/%d/Patch/files_info.json.zip", "BNS_TEST", "up4svr.ncupdate.com"),
    NC_SOFT_AION_CLASSIC_KOREA("http://aionclive.ncupdate.com/%s", "http://aionclive.ncupdate.com/AIONC_LIVE/%d/Patch/files_info.json.zip", "AIONC_LIVE", "up4svr.ncupdate.com"),
    NC_SOFT_BNS_KOREA("http://bnskor.ncupdate.com/%s", "http://bnskor.ncupdate.com/BNS_LIVE/%d/Patch/files_info.json.zip", "BNS_LIVE", "up4svr.ncupdate.com"),
    NC_SOFT_L1_TEST_KOREA("http://l1kortest.ncupdate.com/%s", "http://l1kortest.ncupdate.com/L1RMST_KOR_TEST/%d/Patch/files_info.json.zip", "L1RMST_KOR_TEST", "up4svr.ncupdate.com"),
    NC_SOFT_L1_KOREA("http://l1kor.ncupdate.com/%s", "http://l1kor.ncupdate.com/L1RMST_KOR/%d/Patch/files_info.json.zip", "L1RMST_KOR", "up4svr.ncupdate.com"),
    NC_SOFT_L1_CLONE_KOREA("http://rc-up4rep.ncupdate.com/%s", "http://rc-up4rep.ncupdate.com/L1RMST_CLONE/%d/Patch/files_info.json.zip", "L1RMST_CLONE", "up4svr.ncupdate.com"),
    NC_SOFT_LOVEBEAT_KOREA("http://lbkor.ncupdate.com/%s", "http://lbkor.ncupdate.com/LoveBeatOBT/%d/Patch/files_info.json.zip", "LoveBeatOBT", "up4svr.ncupdate.com"),
    // http://l2-client-cdn.ncsoft.jp/L2_JP/102/Patch/Zip/Maps/24_14.unr.zip
    // http://ncj-L2-asset.ncsoft.jp/L2_JP/212/Patch/Zip/Maps/24_14.unr.zip
    // 101 - 464
    // 102 - 474
    NC_SOFT_JAPANESE("http://ncj-L2-asset.ncsoft.jp/L2_JP/%d/Patch/%s", "http://ncj-L2-asset.ncsoft.jp/L2_JP/%d/Patch/PatchFileInfo_L2_JP_%d.dat", "L2_JP", "l2update.ncsoft.jp"),
    NC_SOFT_L2_QA_JAPAN("http://ncj-L2-asset.ncsoft.jp/QA_L2_JP/%d/Patch/%s", "http://ncj-L2-asset.ncsoft.jp/QA_L2_JP/%d/Patch/PatchFileInfo_QA_L2_JP_%d.dat", "QA_L2_JP", "l2update.ncsoft.jp"),
    NC_SOFT_L2_QA2_JAPAN("http://ncj-L2-asset.ncsoft.jp/QA_L2_JP2/%d/Patch/%s", "http://ncj-L2-asset.ncsoft.jp/QA_L2_JP2/%d/Patch/PatchFileInfo_QA_L2_JP2_%d.dat", "QA_L2_JP2", "l2update.ncsoft.jp"),
    // NC-WEST IS DEAD :D
    // http://d35293xeakkyq4.cloudfront.net/LINEAGE2/479/Patch/FileInfoMap_LINEAGE2_479.dat.zip
    // [Request URI: http://d35293xeakkyq4.cloudfront.net/LINEAGE2/479/Patch/PatchFileInfo_LINEAGE2_479.dat.zip]
    NC_SOFT_AMERICA("http://d35293xeakkyq4.cloudfront.net/LINEAGE2/%d/Patch/%s", "http://d35293xeakkyq4.cloudfront.net/LINEAGE2/%d/Patch/PatchFileInfo_LINEAGE2_%d.dat", "LINEAGE2", "updater.nclauncher.ncsoft.com"),
    NC_SOFT_AION_AMERICA("http://dljhv20e50fab.cloudfront.net/%s", "http://dljhv20e50fab.cloudfront.net/AION/%d/Patch/files_info.json.zip", "AION", "updater.nclauncher.ncsoft.com"),
    NC_SOFT_AION_CLASSIC_AMERICA("http://dljhv20e50fab.cloudfront.net/%s", "http://dljhv20e50fab.cloudfront.net/AION_CLASSIC/%d/Patch/files_info.json.zip", "AION_CLASSIC", "updater.nclauncher.ncsoft.com"),
    NC_SOFT_BNS_UE4_AMERICA("http://d37ob46rk09il3.cloudfront.net/%s", "http://d37ob46rk09il3.cloudfront.net/BnS_UE4/%d/Patch/files_info.json.zip", "BnS_UE4", "updater.nclauncher.ncsoft.com"),
    NC_SOFT_BNS_NEO_AMERICA("http://d37ob46rk09il3.cloudfront.net/%s", "http://d37ob46rk09il3.cloudfront.net/BNSNEO/%d/Patch/files_info.json.zip", "BNSNEO", "updater.nclauncher.ncsoft.com"),

    // Aion2 Windows channels discovered from the public TW service registry. Mobile/store channels are intentionally omitted.
    NC_SOFT_AION2_PURPLE("http://tw-a2-win.cdn.plaync.com.tw/%s", "http://tw-a2-win.cdn.plaync.com.tw/A2_TW_L_GA_PURPLE/%d/Patch/files_info.json.zip", "A2_TW_L_GA_PURPLE", "up4svr.plaync.com.tw"),
    NC_SOFT_AION2_GPG("http://tw-a2-win-g.cdn.plaync.com.tw/%s", "http://tw-a2-win-g.cdn.plaync.com.tw/A2_TW_L_GA_GPG/%d/Patch/files_info.json.zip", "A2_TW_L_GA_GPG", "up4svr.plaync.com.tw"),

    NC_SOFT_BNS_TW_22("http://mmorepo.cdn.plaync.com.tw/TWBNS22/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWBNS22/%d/Patch/PatchFileInfo_TWBNS22_%d.dat", "TWBNS22", "up4svr.plaync.com.tw"),
    NC_SOFT_AION_TW_V47("http://mmorepo.cdn.plaync.com.tw/TWAionV47/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWAionV47/%d/Patch/PatchFileInfo_TWAionV47_%d.dat", "TWAionV47", "up4svr.plaync.com.tw"),
    NC_SOFT_AION_TW_CLASSIC("http://mmorepo.cdn.plaync.com.tw/TWAionVCL_LIVE/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWAionVCL_LIVE/%d/Patch/PatchFileInfo_TWAionVCL_LIVE_%d.dat", "TWAionVCL_LIVE", "up4svr.plaync.com.tw"),
    NC_SOFT_BNS_TW_UE4("http://mmorepo.cdn.plaync.com.tw/TWBNSUE4/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWBNSUE4/%d/Patch/PatchFileInfo_TWBNSUE4_%d.dat", "TWBNSUE4", "up4svr.plaync.com.tw"),
    NC_SOFT_BNS_TW_NEO("http://mmorepo.cdn.plaync.com.tw/TWBNSNEO/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TWBNSNEO/%d/Patch/PatchFileInfo_TWBNSNEO_%d.dat", "TWBNSNEO", "up4svr.plaync.com.tw"),
    NC_SOFT_BNS_TW_TEST_NEO("http://mmorepo.cdn.plaync.com.tw/TESTNEO/%d/Patch/%s", "http://mmorepo.cdn.plaync.com.tw/TESTNEO/%d/Patch/PatchFileInfo_TESTNEO_%d.dat", "TESTNEO", "up4svr.plaync.com.tw"),

    // Akumu HTTP mirror: the file list comes from a .torrent under the configured folder (not a format string);
    // links are built as folder + relative path. See AkumuLinkGenerator / AnubisClient. No NC update protocol.
    AKUMU(null, null, null, null),

    UP_NOVA_LAUNCHER(null, null, null, null),

    /** 4game control.txt manifests are adapted to the common pipeline at runtime. */
    FOUR_GAME(null, null, null, null);

    private final String _generalCdnLink;
    private final String _cdnFileListLink;
    private final String _updateServiceId;
    private final String _updateHost;

    CDNLink(String generalCdnLink, String cdnFileListLink, String updateServiceId, String updateHost)
    {
        _generalCdnLink = generalCdnLink;
        _cdnFileListLink = cdnFileListLink;
        _updateServiceId = updateServiceId;
        _updateHost = updateHost;
    }

    public String getGeneralCdnLink()
    {
        return _generalCdnLink;
    }

    public String getCdnFileListLink()
    {
        return _cdnFileListLink;
    }

    /**
     * EN: The NC update-protocol service id for this region (e.g. {@code L2_JP}), or {@code null} for a source
     *     without an NC update server (Akumu / UpNova). <br>
     * RU: Идентификатор сервиса update-протокола NC для этого региона (напр. {@code L2_JP}) или {@code null} для
     *     источника без update-сервера NC (Akumu / UpNova). <br>
     * @return <br>
     *         {String} - EN: the service id, or null / RU: идентификатор сервиса или null <br>
     **/
    public String getUpdateServiceId()
    {
        return _updateServiceId;
    }

    /**
     * EN: The NC update server host (TCP:27500) for this region, or {@code null} when it is not known yet — then
     *     the live version cannot be resolved for this source. <br>
     * RU: Хост update-сервера NC (TCP:27500) для этого региона или {@code null}, если он ещё не известен — тогда
     *     живую версию для этого источника узнать нельзя. <br>
     * @return <br>
     *         {String} - EN: the update host, or null / RU: хост update-сервера или null <br>
     **/
    public String getUpdateHost()
    {
        return _updateHost;
    }
}
