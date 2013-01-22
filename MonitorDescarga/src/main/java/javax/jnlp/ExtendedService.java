package javax.jnlp;

import java.io.File;
import java.io.IOException;

public abstract interface ExtendedService
{
  public abstract FileContents openFile(File paramFile)
    throws IOException;

  public abstract FileContents[] openFiles(File[] paramArrayOfFile)
    throws IOException;
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.ExtendedService
 * JD-Core Version:    0.6.0
 */