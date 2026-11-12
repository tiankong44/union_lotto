/**
 * 估算长方体鱼缸的标称容积和常见布置下的实际水量范围。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
const LITERS_PER_CUBIC_CENTIMETER = 1000
const COMMON_WATER_GAP_MIN_CM = 1
const COMMON_WATER_GAP_MAX_CM = 3
const COMMON_DISPLACEMENT_MIN_PERCENT = 5
const COMMON_DISPLACEMENT_MAX_PERCENT = 15

/**
 * 根据内侧尺寸、水位预留和底砂造景扣减比例估算鱼缸水量。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
function estimateRectangularTankVolume(input) {
  const length = Number(input?.length)
  const width = Number(input?.width)
  const height = Number(input?.height)
  const waterGapCm = Number(input?.waterGapCm)
  const substrateDepthCm = Number(input?.substrateDepthCm)
  const displacementPercent = Number(input?.displacementPercent)
  if ([input?.length, input?.width, input?.height, input?.waterGapCm, input?.substrateDepthCm, input?.displacementPercent]
    .some((value) => value === '' || value == null)) return null
  if (![length, width, height, waterGapCm, substrateDepthCm, displacementPercent].every(Number.isFinite)) return null
  if (length <= 0 || width <= 0 || height <= COMMON_WATER_GAP_MAX_CM || waterGapCm < 0 || waterGapCm >= height) return null
  if (substrateDepthCm < 0 || substrateDepthCm >= height || waterGapCm + substrateDepthCm / 2 >= height) return null
  if (displacementPercent < 0 || displacementPercent > 100) return null

  // 底砂孔隙会留水，按厚度一半估算其占水高度，再扣除造景占比。
  const effectiveSubstrateDepthCm = substrateDepthCm / 2
  const grossLiters = length * width * height / LITERS_PER_CUBIC_CENTIMETER
  const estimatedLiters = length * width * (height - waterGapCm - effectiveSubstrateDepthCm) / LITERS_PER_CUBIC_CENTIMETER
    * (1 - displacementPercent / 100)
  const estimatedMinLiters = length * width * (height - COMMON_WATER_GAP_MAX_CM - effectiveSubstrateDepthCm) / LITERS_PER_CUBIC_CENTIMETER
    * (1 - COMMON_DISPLACEMENT_MAX_PERCENT / 100)
  const estimatedMaxLiters = length * width * (height - COMMON_WATER_GAP_MIN_CM - effectiveSubstrateDepthCm) / LITERS_PER_CUBIC_CENTIMETER
    * (1 - COMMON_DISPLACEMENT_MIN_PERCENT / 100)
  if (![grossLiters, estimatedLiters, estimatedMinLiters, estimatedMaxLiters].every(Number.isFinite)) return null

  return {
    grossLiters: roundLiter(grossLiters),
    estimatedLiters: roundLiter(estimatedLiters),
    estimatedMinLiters: roundLiter(estimatedMinLiters),
    estimatedMaxLiters: roundLiter(estimatedMaxLiters)
  }
}

/** 将容量显示统一保留一位小数。 @Author zhanghao_SMEICS @Date 2026-11-12 */
function roundLiter(value) {
  return Math.round(value * 10) / 10
}

module.exports = { estimateRectangularTankVolume }
