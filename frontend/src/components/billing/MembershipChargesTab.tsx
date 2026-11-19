"use client";

import { useEffect, useMemo } from "react";
import { ChargesWorkspace } from "@/components/billing/ChargesWorkspace";
import { ChargesViewModel } from "@/features/billing/viewmodel/ChargesViewModel";

/** "Cobranças" tab of the membership detail: charges of this membership only. */
export function MembershipChargesTab({ membershipId, studentId }: { membershipId: string; studentId: string }) {
  const viewModel = useMemo(() => new ChargesViewModel(), []);

  useEffect(() => {
    viewModel.init({ showSummary: false, membershipId, studentId });
  }, [viewModel, membershipId, studentId]);

  return <ChargesWorkspace viewModel={viewModel} compact />;
}
