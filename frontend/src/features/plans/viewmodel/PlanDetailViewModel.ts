import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { planApi } from "@/features/plans/data/PlanApi";
import {
  EMPTY_FEATURE_FORM,
  EMPTY_PLAN_FORM,
  EMPTY_PRICE_FORM,
  featureToInput,
  planToInput,
  type FeatureInput,
  type PlanDetail,
  type PlanInput,
  type PlanPrice,
  type PlanStatus,
  type PriceInput,
} from "@/features/plans/model/types";
import { validateFeatures, validatePlan, validatePrice, type FormErrors } from "@/features/plans/model/validation";

export type PlanModal = "none" | "edit" | "price" | "cancelPrice";

export interface PlanDetailState extends FeedbackState {
  loading: boolean;
  plan: PlanDetail | null;
  prices: PlanPrice[];
  branches: BranchSummary[];
  modal: PlanModal;
  form: PlanInput;
  formErrors: FormErrors;
  priceForm: PriceInput;
  cancelPriceId: string | null;
  cancelReason: string;
  features: FeatureInput[];
  featuresDirty: boolean;
}

const initialState: PlanDetailState = {
  loading: true,
  plan: null,
  prices: [],
  branches: [],
  modal: "none",
  form: { ...EMPTY_PLAN_FORM },
  formErrors: {},
  priceForm: { ...EMPTY_PRICE_FORM },
  cancelPriceId: null,
  cancelReason: "",
  features: [],
  featuresDirty: false,
  error: null,
  success: null,
  saving: false,
};

export class PlanDetailViewModel extends FeedbackViewModel<PlanDetailState> {
  private planId: string | null = null;

  constructor() {
    super(initialState);
  }

  async load(id: string): Promise<void> {
    this.planId = id;
    this.setState({ loading: true, error: null });
    try {
      const [plan, prices] = await Promise.all([planApi.detail(id), planApi.prices(id)]);
      this.applyPlan(plan);
      this.setState({ prices, loading: false });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
    try {
      const branches = await branchApi.list({ page: 0, size: 100 });
      this.setState({ branches: branches.content });
    } catch {
      // Only needed for BRANCH_ACCESS benefits.
    }
  }

  private applyPlan(plan: PlanDetail) {
    this.setState({ plan, features: plan.features.map(featureToInput), featuresDirty: false });
  }

  private async reloadPrices(): Promise<void> {
    if (!this.planId) return;
    const [plan, prices] = await Promise.all([planApi.detail(this.planId), planApi.prices(this.planId)]);
    this.setState({ plan, prices });
  }

  closeModal = () => this.setState({ modal: "none", formErrors: {} });

  openEdit = () => {
    const plan = this.state.plan;
    if (!plan) return;
    this.setState({ modal: "edit", form: planToInput(plan), formErrors: {}, error: null });
  };

  updateForm = (patch: Partial<PlanInput>) => this.setState({ form: { ...this.state.form, ...patch } });

  async saveEdit(): Promise<void> {
    const plan = this.state.plan;
    if (!plan) return;
    const formErrors = validatePlan(this.state.form, false);
    this.setState({ formErrors });
    if (Object.keys(formErrors).length > 0) return;
    await this.run(async () => {
      this.applyPlan(await planApi.update(plan.id, this.state.form, plan.version));
      this.setState({ modal: "none" });
    }, "Plano atualizado. Matrículas existentes mantêm as condições contratadas.");
  }

  async changeStatus(status: PlanStatus): Promise<void> {
    const plan = this.state.plan;
    if (!plan) return;
    await this.run(async () => this.applyPlan(await planApi.changeStatus(plan.id, status)), "Status atualizado.");
  }

  openPrice = () =>
    this.setState({
      modal: "price",
      priceForm: {
        ...EMPTY_PRICE_FORM,
        billingPeriod: this.state.plan?.currentPrice?.billingPeriod ?? "MONTHLY",
        billingInterval: String(this.state.plan?.currentPrice?.billingInterval ?? 1),
      },
      formErrors: {},
      error: null,
    });

  updatePriceForm = (patch: Partial<PriceInput>) =>
    this.setState({ priceForm: { ...this.state.priceForm, ...patch } });

  async savePrice(): Promise<void> {
    const plan = this.state.plan;
    if (!plan) return;
    const formErrors = validatePrice(this.state.priceForm);
    this.setState({ formErrors });
    if (Object.keys(formErrors).length > 0) return;
    await this.run(async () => {
      await planApi.addPrice(plan.id, this.state.priceForm);
      await this.reloadPrices();
      this.setState({ modal: "none" });
    }, "Novo preço programado. O preço anterior foi encerrado na véspera.");
  }

  openCancelPrice = (priceId: string) =>
    this.setState({ modal: "cancelPrice", cancelPriceId: priceId, cancelReason: "", error: null });

  setCancelReason = (cancelReason: string) => this.setState({ cancelReason });

  async cancelPrice(): Promise<void> {
    const plan = this.state.plan;
    const priceId = this.state.cancelPriceId;
    if (!plan || !priceId) return;
    if (!this.state.cancelReason.trim()) {
      this.setState({ error: "Informe o motivo do cancelamento." });
      return;
    }
    await this.run(async () => {
      await planApi.cancelPrice(plan.id, priceId, this.state.cancelReason);
      await this.reloadPrices();
      this.setState({ modal: "none", cancelPriceId: null });
    }, "Preço programado cancelado.");
  }

  addFeature = () =>
    this.setState({ features: [...this.state.features, { ...EMPTY_FEATURE_FORM }], featuresDirty: true });

  updateFeature = (index: number, patch: Partial<FeatureInput>) =>
    this.setState({
      features: this.state.features.map((f, i) => (i === index ? { ...f, ...patch } : f)),
      featuresDirty: true,
    });

  toggleWeekday = (index: number, bit: number) => {
    const feature = this.state.features[index];
    if (feature) this.updateFeature(index, { daysOfWeek: feature.daysOfWeek ^ bit });
  };

  removeFeature = (index: number) =>
    this.setState({ features: this.state.features.filter((_, i) => i !== index), featuresDirty: true });

  resetFeatures = () => {
    const plan = this.state.plan;
    if (plan) this.applyPlan(plan);
  };

  async saveFeatures(): Promise<void> {
    const plan = this.state.plan;
    if (!plan) return;
    const problem = validateFeatures(this.state.features);
    if (problem) {
      this.setState({ error: problem });
      return;
    }
    await this.run(async () => this.applyPlan(await planApi.replaceFeatures(plan.id, this.state.features)),
      "Benefícios atualizados.");
  }
}
