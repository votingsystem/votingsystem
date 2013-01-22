package javax.jnlp;

import java.awt.datatransfer.Transferable;

public abstract interface ClipboardService
{
  public abstract Transferable getContents();

  public abstract void setContents(Transferable paramTransferable);
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.ClipboardService
 * JD-Core Version:    0.6.0
 */