"use client";

import { useSyncExternalStore } from "react";
import type { ViewModel } from "@/core/mvvm/ViewModel";

export function useViewModel<S extends object>(viewModel: ViewModel<S>): S {
  return useSyncExternalStore(
    viewModel.subscribe,
    viewModel.getSnapshot,
    viewModel.getSnapshot,
  );
}
