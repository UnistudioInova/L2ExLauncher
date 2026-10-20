package org.l2explorer.launcher.backend.model.linkgenerator;

import org.l2explorer.launcher.backend.enums.CDNLink;

public class NcAmericaLinkGenerator extends NcTaiwanLinkGenerator
{
    protected NcAmericaLinkGenerator(int patchVersion)
    {
        super(CDNLink.NC_SOFT_AMERICA, patchVersion);
    }

    @Override
    protected String getFileListFileName()
    {
        return "PatchFileInfo_LINEAGE2_" + _patchVersion + ".dat";
    }

    @Override
    protected String getFileListFileHash()
    {
        return "FileInfoMap_LINEAGE2_" + _patchVersion + ".dat";
    }
}
