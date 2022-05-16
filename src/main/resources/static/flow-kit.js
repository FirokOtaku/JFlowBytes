
/**
 * 创建一个按照指定切片大小返回文件切片的迭代器
 * @param file {Blob} 文件
 * @param sliceSize {number} 切片大小. 不传默认为 5 MB
 * @returns {Iterator<Blob>} 切片迭代器
 * @example
 * for(const slice of slicesOf(file)) { ... }
 * */
function* slicesOf(file, sliceSize = 1024 * 1024 * 5)
{
    const fileSize = file.size;
    const sliceCount = parseInt(fileSize / sliceSize + '') + (fileSize % sliceSize === 0 ? 0 : 1);
    for(let sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++)
    {
        // 计算切片的起始位置
        const posStart = sliceIndex * sliceSize;
        const posStop = Math.min(fileSize, posStart + sliceSize);
        // 进行一个片的切
        const filePart = file.slice(posStart, posStop);

        yield filePart;
    }
}

