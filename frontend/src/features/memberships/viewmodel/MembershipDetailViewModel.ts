import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { normalizeMoney } from "@/core/validation";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { contractApi } from "@/features/contracts/data/ContractApi";
import type { ContractSummary } from "@/features/contracts/model/types";
import { membershipApi } from "@/features/memberships/data/MembershipApi";
import {
  EMPTY_RENEW_FORM,
  type MembershipDetail,
  type MembershipHistoryEntry,
  type RenewInput,
} from "@/features/memberships/model/types";

export type MembershipTab = "overview" | "freezes" | "contracts" | "history";
export type ReasonAction = "cancel" | "suspend" | "reactivate" | "unfreeze";
export type MembershipModal = "none" | ReasonAction | "freeze" | "renew" | "edit";

export interface MembershipDetailState extends FeedbackState {
  loading: boolean;
  membership: MembershipDetail | null;
  history: MembershipHistoryEntry[];
  contracts: ContractSummary[];
  branches: BranchSummary[];
  tab: MembershipTab;
  modal: MembershipModal;
  reason: string;
  freezeEndDate: string;
  renewForm: RenewInput;
  editBranchId: string;
  editNotes: string;
  fieldErrors: Record<string, string>;
}

const initialState: MembershipDetailState = {
  loading: true,
  membership: null,
  history: [],
  contracts: [],
  branches: [],
  tab: "overview",
  modal: "none",
  reason: "",
  freezeEndDate: "",
  renewForm: { ...EMPTY_RENEW_FORM },
  editBranchId: "",
  editNotes: "",
  fieldErrors: {},
  error: null,
  success: null,
  saving: false,
};

const REASON_MESSAGES: Record<ReasonAction, string> = {
  cancel: "Matrícula cancelada.",
  suspend: "Matrícula suspensa.",
  reactivate: "Matrícula reativada.",
  unfreeze: "Matrícula descongelada. Dias não usados foram devolvidos.",
};

export class MembershipDetailViewModel extends FeedbackViewModel<MembershipDetailState> {
  private membershipId: string | null = null;

  constructor() {
    super(initialState);
  }

  async load(id: string): Promise<void> {
    this.membershipId = id;
    this.setState({ loading: true, error: null, modal: "none" });
    try {
      const membership = await membershipApi.detail(id);
      this.setState({ membership, loading: false });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
      return;
    }
    await Promise.allSettled([this.loadHistory(), this.loadContracts(), this.loadBranches()]);
  }

  private async loadHistory(): Promise<void> {
    if (!this.membershipId) return;
    try {
      this.setState({ history: await membershipApi.history(this.membershipId) });
    } catch {
      // History is secondary; the tab shows an empty state.
    }
  }

  private async loadContracts(): Promise<void> {
    if (!this.membershipId) return;
    try {
      this.setState({ contracts: await contractApi.history(this.membershipId) });
    } catch {
      // Users without contracts:read still see the membership.
    }
  }

  private async loadBranches(): Promise<void> {
    try {
      this.setState({ branches: (await branchApi.list({ status: "ACTIVE", page: 0, size: 100 })).content });
    } catch {
      // Only needed for edit/renew.
    }
  }

  private async applyMembership(membership: MembershipDetail): Promise<void> {
    this.setState({ membership, modal: "none" });
    await this.loadHistory();
  }

  setTab = (tab: MembershipTab) => this.setState({ tab });

  openModal = (modal: MembershipModal) => {
    const membership = this.state.membership;
    this.setState({
      modal,
      reason: "",
      freezeEndDate: "",
      fieldErrors: {},
      error: null,
      renewForm: { ...EMPTY_RENEW_FORM, branchId: membership?.branchId ?? "" },
      editBranchId: membership?.branchId ?? "",
      editNotes: membership?.notes ?? "",
    });
  };

  closeModal = () => this.setState({ modal: "none" });
  setReason = (reason: string) => this.setState({ reason });
  setFreezeEndDate = (freezeEndDate: string) => this.setState({ freezeEndDate });
  setEditBranch = (editBranchId: string) => this.setState({ editBranchId });
  setEditNotes = (editNotes: string) => this.setState({ editNotes });
  updateRenew = (patch: Partial<RenewInput>) => this.setState({ renewForm: { ...this.state.renewForm, ...patch } });

  async submitReason(action: ReasonAction): Promise<void> {
    const id = this.membershipId;
    if (!id) return;
    if (action !== "unfreeze" && !this.state.reason.trim()) {
      this.setState({ fieldErrors: { reason: "Informe o motivo." } });
      return;
    }
    const reason = this.state.reason;
    await this.run(async () => {
      const calls = {
        cancel: () => membershipApi.cancel(id, reason),
        suspend: () => membershipApi.suspend(id, reason),
        reactivate: () => membershipApi.reactivate(id, reason),
        unfreeze: () => membershipApi.unfreeze(id, reason),
      };
      await this.applyMembership(await calls[action]());
    }, REASON_MESSAGES[action]);
  }

  async submitFreeze(): Promise<void> {
    const id = this.membershipId;
    if (!id) return;
    const fieldErrors: Record<string, string> = {};
    if (!this.state.freezeEndDate) fieldErrors.freezeEndDate = "Informe a data final do congelamento.";
    if (!this.state.reason.trim()) fieldErrors.reason = "Informe o motivo.";
    this.setState({ fieldErrors });
    if (Object.keys(fieldErrors).length > 0) return;
    await this.run(async () => {
      await this.applyMembership(await membershipApi.freeze(id, this.state.freezeEndDate, this.state.reason));
    }, "Matrícula congelada. A vigência foi estendida pelos dias planejados.");
  }

  /** Returns the id of the renewal so the page can navigate to it. */
  async submitRenew(): Promise<string | null> {
    const id = this.membershipId;
    if (!id) return null;
    const form = this.state.renewForm;
    const fieldErrors: Record<string, string> = {};
    if (form.discountAmount.trim()) {
      if (!normalizeMoney(form.discountAmount)) fieldErrors.discountAmount = "Valor inválido (até 2 casas).";
      if (!form.discountReason.trim()) fieldErrors.discountReason = "Informe o motivo do desconto.";
    }
    this.setState({ fieldErrors });
    if (Object.keys(fieldErrors).length > 0) return null;
    let renewalId: string | null = null;
    await this.run(async () => {
      renewalId = (await membershipApi.renew(id, form)).id;
      this.setState({ modal: "none" });
    }, "Renovação criada com o preço vigente do plano.");
    return renewalId;
  }

  async submitEdit(): Promise<void> {
    const membership = this.state.membership;
    if (!membership) return;
    if (!this.state.editBranchId) {
      this.setState({ fieldErrors: { editBranchId: "Selecione a unidade." } });
      return;
    }
    await this.run(async () => {
      await this.applyMembership(
        await membershipApi.update(membership.id, this.state.editBranchId, this.state.editNotes, membership.version),
      );
    }, "Matrícula atualizada.");
  }

  /** Returns the generated contract id so the page can open it. */
  async generateContract(): Promise<string | null> {
    const id = this.membershipId;
    if (!id) return null;
    let contractId: string | null = null;
    await this.run(async () => {
      contractId = (await contractApi.generate(id)).id;
      await Promise.all([this.loadContracts(), this.loadHistory()]);
    }, "Contrato gerado.");
    return contractId;
  }
}
