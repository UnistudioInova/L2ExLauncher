package org.l2explorer.launcher.backend.model.holders;

public class LinkInfoHolder
{
    private final FileInfoHolder _fileInfo;

    private String _accessLink;
    private int _httpStatus;
    private long _httpLength;
    private long _rangeStart = -1L;
    private long _rangeEnd = -1L;

    public LinkInfoHolder(FileInfoHolder fileInfo)
    {
        _fileInfo = fileInfo;
        _httpStatus = -1;
        _httpLength = -1L;
    }

    public FileInfoHolder getFileInfo()
    {
        return _fileInfo;
    }

    public String getAccessLink()
    {
        return _accessLink;
    }

    public void setAccessLink(String accessLink)
    {
        _accessLink = accessLink;
    }

    public int getHttpStatus()
    {
        return _httpStatus;
    }

    public void setHttpStatus(int httpStatus)
    {
        _httpStatus = httpStatus;
    }

    public long getHttpLength()
    {
        return _httpLength;
    }

    public void setHttpLength(long httpLength)
    {
        _httpLength = httpLength;
    }

    /** Optional inclusive byte range used by manifests such as 4game control.txt. */
    public long getRangeStart() { return _rangeStart; }
    public long getRangeEnd() { return _rangeEnd; }
    public void setRange(long start, long end)
    {
        _rangeStart = start;
        _rangeEnd = end;
    }
    public boolean hasRange() { return _rangeStart >= 0 && _rangeEnd >= _rangeStart; }
}
