export function createEditorAutosave({
  delay = 1200,
  setTimer = setTimeout,
  clearTimer = clearTimeout,
} = {}) {
  let timer;
  let generation = 0;
  let activeKey = null;

  function cancel() {
    generation += 1;
    activeKey = null;
    if (timer !== undefined) clearTimer(timer);
    timer = undefined;
  }

  function schedule(key, save) {
    cancel();
    const scheduledGeneration = generation;
    activeKey = key;
    timer = setTimer(async () => {
      if (scheduledGeneration !== generation || activeKey !== key) return;
      timer = undefined;
      await save();
    }, delay);
  }

  return { cancel, schedule };
}
