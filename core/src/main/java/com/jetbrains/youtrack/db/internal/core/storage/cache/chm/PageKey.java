package com.jetbrains.youtrack.db.internal.core.storage.cache.chm;

public final class PageKey {

  private final long fileId;
  private final int pageIndex;

  private final int hash;

  public PageKey(final long fileId, final int pageIndex) {
    this.fileId = fileId;
    this.pageIndex = pageIndex;
    this.hash = hashCode(fileId, pageIndex);
  }

  public long getFileId() {
    return fileId;
  }

  public int getPageIndex() {
    return pageIndex;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final var pageKey = (PageKey) o;

    if (fileId != pageKey.fileId) {
      return false;
    }
    return pageIndex == pageKey.pageIndex;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "PageKey{" + "fileId=" + fileId + ", pageIndex=" + pageIndex + '}';
  }

  public static int hashCode(final long fileId, final int pageIndex) {
    var result = (int) (fileId ^ (fileId >>> 32));
    result = 31 * result + pageIndex;
    return result;
  }
}
