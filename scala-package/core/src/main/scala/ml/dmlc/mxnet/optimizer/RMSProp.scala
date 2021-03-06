package ml.dmlc.mxnet.optimizer

import ml.dmlc.mxnet.{NDArray, Optimizer, LRScheduler}
import ml.dmlc.mxnet.NDArrayConversions._

/**
  * RMSProp optimizer as described in Tieleman & Hinton, 2012.
  * http://arxiv.org/pdf/1308.0850v5.pdf Eq(38) - Eq(45) by Alex Graves, 2013.
  *
  * @author Yuan Tang
  *
  * @param learningRate Float, Step size.
  * @param gamma1 Float, decay factor of moving average for gradient, gradient^^2.
  * @param gamma2 Float, momentum factor of moving average for gradient.
  * @param rescaleGradient Float, rescaling factor of gradient.
  * @param wd Float, L2 regularization coefficient add to all the weights
  * @param clipGradient Float, clip gradient in range [-clip_gradient, clip_gradient]
  * @param lrScheduler The learning rate scheduler
  */
class RMSProp(val learningRate: Float = 0.05f, val rescaleGradient: Float = 1.0f,
              val gamma1: Float = 0.95f, val gamma2: Float = 0.9f, val wd: Float = 0.0f,
              val lrScheduler: LRScheduler = null, val clipGradient: Float = 0f) extends Optimizer {

  /**
    * Update the parameters.
    * @param index An unique integer key used to index the parameters
    * @param weight weight ndarray
    * @param grad grad ndarray
    * @param state NDArray or other objects returned by initState
    *              The auxiliary state used in optimization.
    */
  override def update(index: Int, weight: NDArray, grad: NDArray, state: AnyRef): Unit = {
    val lr = this.learningRate * lrScale.getOrElse(index, 1f)
    val (n, g, delta) = state.asInstanceOf[(NDArray, NDArray, NDArray)]

    val wd =
      if (specialized) {
        if (this.weightSet.contains(index)) {
          this.wd
        } else {
          0f
        }
      } else {
        this.wd
      }

    var resdGrad = grad * this.rescaleGrad

    if (clipGradient != 0f) {
      val oldResdGrad = resdGrad
      resdGrad = NDArray.clip(resdGrad, -clipGradient, clipGradient)
      oldResdGrad.dispose()
    }

    n.set((1 - this.gamma1) * (resdGrad * resdGrad) + this.gamma1 * n)
    g.set((1 - this.gamma1) * resdGrad + this.gamma1 * g)
    delta.set(this.gamma2 * delta - lr * (resdGrad / NDArray.sqrt(n - g * g + 1e-4f) + wd * weight))
    weight += delta

    resdGrad.dispose()
  }

  override def createState(index: Int, weight: NDArray): (NDArray, NDArray, NDArray) = {
    (NDArray.zeros(weight.shape, weight.context), // n
      NDArray.zeros(weight.shape, weight.context), // g
        NDArray.zeros(weight.shape, weight.context)) // delta
  }
}

