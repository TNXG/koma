import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { fileIo } from '@kit.CoreFileKit';
import { hilog } from '@kit.PerformanceAnalysisKit';

const DOMAIN = 0x0000;

/** 打开系统相册选择器（单选、读原图），选中后回传 JPEG 原始字节；取消或失败回传空缓冲。 */
export async function pickPhotoBytes(context: Context | undefined, id: number,
  complete: (id: number, data: ArrayBuffer) => void): Promise<void> {
  const empty = new ArrayBuffer(0);
  if (!context) {
    hilog.warn(DOMAIN, 'KomaPhoto', 'Host context unavailable for photo picker');
    complete(id, empty);
    return;
  }
  try {
    const picker = new photoAccessHelper.PhotoViewPicker();
    const options = new photoAccessHelper.PhotoSelectOptions();
    options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
    options.maxSelectNumber = 1;
    const result = await picker.select(options);
    const uri = result.photoUris.length > 0 ? result.photoUris[0] : undefined;
    if (!uri) {
      complete(id, empty);
      return;
    }
    const handle = await fileIo.open(uri, fileIo.OpenMode.READ_ONLY);
    try {
      const stat = await fileIo.stat(handle.fd);
      if (stat.size <= 0) {
        complete(id, empty);
        return;
      }
      const buffer = new ArrayBuffer(stat.size);
      const read = await fileIo.read(handle.fd, buffer);
      const bytes = read === stat.size ? buffer : buffer.slice(0, Math.max(read, 0));
      hilog.info(DOMAIN, 'KomaPhoto', 'Photo picked: %{public}d bytes', bytes.byteLength);
      complete(id, bytes);
    } finally {
      await fileIo.close(handle.fd);
    }
  } catch (error) {
    hilog.warn(DOMAIN, 'KomaPhoto', 'Photo pick failed: %{public}s', (error as Error).message);
    complete(id, empty);
  }
}
