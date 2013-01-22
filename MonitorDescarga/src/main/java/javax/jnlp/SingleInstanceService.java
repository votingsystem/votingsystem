package javax.jnlp;

public abstract interface SingleInstanceService
{
  public abstract void addSingleInstanceListener(SingleInstanceListener paramSingleInstanceListener);

  public abstract void removeSingleInstanceListener(SingleInstanceListener paramSingleInstanceListener);
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.SingleInstanceService
 * JD-Core Version:    0.6.0
 */