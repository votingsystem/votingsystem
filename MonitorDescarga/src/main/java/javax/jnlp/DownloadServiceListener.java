package javax.jnlp;

import java.net.URL;

public abstract interface DownloadServiceListener
{
  public abstract void progress(URL paramURL, String paramString, long paramLong1, long paramLong2, int paramInt);

  public abstract void validating(URL paramURL, String paramString, long paramLong1, long paramLong2, int paramInt);

  public abstract void upgradingArchive(URL paramURL, String paramString, int paramInt1, int paramInt2);

  public abstract void downloadFailed(URL paramURL, String paramString);
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.DownloadServiceListener
 * JD-Core Version:    0.6.0
 */