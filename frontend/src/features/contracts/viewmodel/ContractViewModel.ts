import { sha256Hex, saveBlob } from "@/core/crypto";
import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { contractApi } from "@/features/contracts/data/ContractApi";
import type { ContractView } from "@/features/contracts/model/types";

export interface ContractState extends FeedbackState {
  loading: boolean;
  contract: ContractView | null;
  /** Hash of the content rendered on screen, computed locally. */
  displayedHash: string | null;
  hashUnavailable: boolean;
  downloading: boolean;
  acceptOpen: boolean;
  acceptorName: string;
  confirmed: boolean;
  fieldErrors: Record<string, string>;
}

const initialState: ContractState = {
  loading: true,
  contract: null,
  displayedHash: null,
  hashUnavailable: false,
  downloading: false,
  acceptOpen: false,
  acceptorName: "",
  confirmed: false,
  fieldErrors: {},
  error: null,
  success: null,
  saving: false,
};

/** True when what the operator sees matches what the backend stored. */
export function integrityOk(state: Pick<ContractState, "contract" | "displayedHash">): boolean {
  const { contract, displayedHash } = state;
  return !!contract && (displayedHash === null || displayedHash === contract.contentSha256);
}

export class ContractViewModel extends FeedbackViewModel<ContractState> {
  constructor() {
    super(initialState);
  }

  async load(id: string): Promise<void> {
    this.setState({ loading: true, error: null });
    try {
      await this.applyContract(await contractApi.detail(id));
      this.setState({ loading: false });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  }

  private async applyContract(contract: ContractView): Promise<void> {
    const displayedHash = await sha256Hex(contract.content);
    this.setState({ contract, displayedHash, hashUnavailable: displayedHash === null });
  }

  async download(): Promise<void> {
    const contract = this.state.contract;
    if (!contract) return;
    this.setState({ downloading: true, error: null });
    try {
      const blob = await contractApi.document(contract.id);
      saveBlob(blob, `contrato-v${contract.contractVersion}-${contract.id.slice(0, 8)}.pdf`);
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    } finally {
      this.setState({ downloading: false });
    }
  }

  openAccept = () =>
    this.setState({ acceptOpen: true, acceptorName: "", confirmed: false, fieldErrors: {}, error: null });
  closeAccept = () => this.setState({ acceptOpen: false });
  setAcceptorName = (acceptorName: string) => this.setState({ acceptorName });
  setConfirmed = (confirmed: boolean) => this.setState({ confirmed });

  async accept(): Promise<void> {
    const { contract, displayedHash } = this.state;
    if (!contract) return;
    const fieldErrors: Record<string, string> = {};
    if (!this.state.acceptorName.trim()) fieldErrors.acceptorName = "Informe o nome de quem aceita.";
    if (!this.state.confirmed) fieldErrors.confirmed = "Confirme que o aluno leu e aceitou o conteúdo.";
    this.setState({ fieldErrors });
    if (Object.keys(fieldErrors).length > 0) return;
    if (!integrityOk(this.state)) {
      this.setState({ error: "O conteúdo exibido não confere com o registrado. Recarregue a página." });
      return;
    }
    // Without SubtleCrypto (plain HTTP) we fall back to the hash the server sent with the displayed content.
    const hash = displayedHash ?? contract.contentSha256;
    await this.run(async () => {
      await this.applyContract(await contractApi.accept(contract.id, this.state.acceptorName, hash, true));
      this.setState({ acceptOpen: false });
    }, "Aceite registrado.");
  }
}
