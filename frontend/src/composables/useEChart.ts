import { nextTick, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'

export const useEChart = () => {
  const elementRef = ref<HTMLDivElement>()
  let chart: echarts.ECharts | null = null

  const ensureChart = async () => {
    await nextTick()
    if (elementRef.value && !chart) {
      chart = echarts.init(elementRef.value)
    }
    return chart
  }

  const setOption = async (option: echarts.EChartsCoreOption) => {
    const instance = await ensureChart()
    instance?.setOption(option, true)
  }

  const resize = () => {
    chart?.resize()
  }

  onBeforeUnmount(() => {
    chart?.dispose()
    chart = null
  })

  return {
    elementRef,
    setOption,
    resize,
  }
}
