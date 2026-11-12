/**
 * 选择实况照片的静态画面和对应动态片段，并把临时路径交还给所属表单。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
Component({
  properties: {
    imagePath: { type: String, value: '' },
    motionPath: { type: String, value: '' },
    imageLocked: { type: String, value: '' },
    disabled: { type: Boolean, value: false }
  },

  methods: {
    /** 从相册或相机选择 Live Photo 的静态画面。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    chooseImage() {
      if (this.data.disabled || this.data.imageLocked) return
      // 只选择静态画面，避免视频路径覆盖照片预览。
      wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], sizeType: ['compressed'],
        success: (result) => this.emitSelectedPath('imagePath', result),
        fail: (error) => this.showPickerError(error, '静态照片') })
    },

    /** 从相册或相机选择与静态画面对应的动态片段。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    chooseMotion() {
      if (this.data.disabled) return
      // 选择视频文件作为实况照片动效，后续由服务端保存至共享云端。
      wx.chooseMedia({ count: 1, mediaType: ['video'], sourceType: ['album', 'camera'], maxDuration: 30,
        success: (result) => this.emitSelectedPath('motionPath', result),
        fail: (error) => this.showPickerError(error, '动态片段') })
    },

    /** 将选择器返回的临时文件路径交给表单页面。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    emitSelectedPath(field, result) {
      const file = result?.tempFiles?.[0]
      const filePath = file?.tempFilePath ?? file?.path ?? result?.tempFilePaths?.[0]
      if (filePath) this.triggerEvent('change', { field, value: filePath })
      else wx.showToast({ title: '没有读取到所选文件，请重新选择', icon: 'none' })
    },

    /** 用户取消选择时保持安静，其他系统选择器错误提供明确提示。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    showPickerError(error, label) {
      if (String(error?.errMsg ?? '').toLowerCase().includes('cancel')) return
      wx.showToast({ title: `${label}暂时无法读取，请重试`, icon: 'none' })
    },

    /** 清除尚未保存的静态画面。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    removeImage() {
      if (!this.data.disabled && !this.data.imageLocked) this.triggerEvent('change', { field: 'imagePath', value: '' })
    },

    /** 清除尚未保存的动态片段。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    removeMotion() {
      if (!this.data.disabled) this.triggerEvent('change', { field: 'motionPath', value: '' })
    }
  }
})
