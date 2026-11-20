// Greedy CTC decoding: collapse repeated frames, with blank/unlisted classes separating runs.
export function decodeCaptcha(scores: Float32Array, classes: number, charset: Map<number, string>): string {
  if (classes <= 0 || scores.length % classes !== 0) {
    throw new Error('Invalid captcha output shape');
  }
  let previous = -1;
  let text = '';
  for (let base = 0; base < scores.length; base += classes) {
    let best = 0;
    for (let index = 1; index < classes; index++) {
      if (scores[base + index] > scores[base + best]) {
        best = index;
      }
    }
    if (best !== previous) {
      text += charset.get(best) ?? '';
    }
    previous = best;
  }
  return text;
}
