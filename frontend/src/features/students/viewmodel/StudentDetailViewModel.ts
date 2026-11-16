import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { studentApi } from "@/features/students/data/StudentApi";
import {
  EMPTY_ADDRESS_FORM,
  EMPTY_CONTACT_FORM,
  EMPTY_DOCUMENT_FORM,
  type AddressInput,
  type ContactInput,
  type DocumentInput,
  type StudentAddress,
  type StudentBranchAssignment,
  type StudentContact,
  type StudentDetail,
  type StudentDocument,
  type StudentHistoryEntry,
  type StudentNote,
  type StudentStatus,
} from "@/features/students/model/types";

export type DetailTab = "overview" | "documents" | "contacts" | "address" | "branches" | "history" | "notes";

export interface StudentDetailState {
  loading: boolean;
  studentId: string | null;
  student: StudentDetail | null;
  tab: DetailTab;
  history: StudentHistoryEntry[];
  notes: StudentNote[];
  address: StudentAddress | null;
  branches: BranchSummary[];
  error: string | null;
  success: string | null;
  saving: boolean;
  revealSensitive: boolean;
  documentForm: DocumentInput;
  editingDocumentId: string | null;
  contactForm: ContactInput;
  editingContactId: string | null;
  addressForm: AddressInput;
  noteContent: string;
  assignBranchId: string;
  assignPrimary: boolean;
  transferBranchId: string;
  transferReason: string;
}

const initialState: StudentDetailState = {
  loading: true,
  studentId: null,
  student: null,
  tab: "overview",
  history: [],
  notes: [],
  address: null,
  branches: [],
  error: null,
  success: null,
  saving: false,
  revealSensitive: false,
  documentForm: { ...EMPTY_DOCUMENT_FORM },
  editingDocumentId: null,
  contactForm: { ...EMPTY_CONTACT_FORM },
  editingContactId: null,
  addressForm: { ...EMPTY_ADDRESS_FORM },
  noteContent: "",
  assignBranchId: "",
  assignPrimary: false,
  transferBranchId: "",
  transferReason: "",
};

export class StudentDetailViewModel extends ViewModel<StudentDetailState> {
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    super(initialState);
  }

  async load(id: string): Promise<void> {
    this.setState({ loading: true, error: null, studentId: id });
    try {
      const [student, history, notes, branches, address] = await Promise.all([
        studentApi.detail(id),
        studentApi.history(id).catch(() => [] as StudentHistoryEntry[]),
        studentApi.listNotes(id).catch(() => [] as StudentNote[]),
        branchApi.list({ page: 0, size: 100 }).then((r) => r.content).catch(() => [] as BranchSummary[]),
        studentApi.getAddress(id).catch(() => null),
      ]);
      this.setState({
        loading: false,
        student,
        history,
        notes,
        branches,
        address,
        addressForm: address
          ? {
              zipCode: address.zipCode ?? "",
              street: address.street ?? "",
              number: address.number ?? "",
              complement: address.complement ?? "",
              neighborhood: address.neighborhood ?? "",
              city: address.city ?? "",
              state: address.state ?? "",
            }
          : { ...EMPTY_ADDRESS_FORM },
      });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  }

  setTab = (tab: DetailTab) => this.setState({ tab, error: null, success: null });

  async refresh(): Promise<void> {
    const id = this.state.studentId;
    if (!id) return;
    await this.load(id);
  }

  async changeStatus(status: StudentStatus): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.changeStatus(this.state.studentId, status);
      this.flashSuccess("Status atualizado.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  async toggleReveal(): Promise<void> {
    if (!this.state.studentId) return;
    const next = !this.state.revealSensitive;
    this.setState({ saving: true, error: null });
    try {
      const documents = await studentApi.listDocuments(this.state.studentId, next);
      this.setState({
        revealSensitive: next,
        saving: false,
        student: this.state.student ? { ...this.state.student, documents } : null,
      });
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    }
  }

  setDocumentField = (field: keyof DocumentInput, value: string | boolean) =>
    this.setState({ documentForm: { ...this.state.documentForm, [field]: value } as DocumentInput });

  editDocument = (doc: StudentDocument) =>
    this.setState({
      editingDocumentId: doc.id,
      documentForm: {
        type: doc.type,
        number: doc.number,
        issuer: doc.issuer ?? "",
        state: doc.state ?? "",
        issuedAt: doc.issuedAt ?? "",
        expiresAt: doc.expiresAt ?? "",
        notes: doc.notes ?? "",
        primary: doc.primary,
      },
    });

  resetDocumentForm = () =>
    this.setState({ editingDocumentId: null, documentForm: { ...EMPTY_DOCUMENT_FORM } });

  async saveDocument(): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      if (this.state.editingDocumentId) {
        await studentApi.updateDocument(this.state.studentId, this.state.editingDocumentId, this.state.documentForm);
      } else {
        await studentApi.addDocument(this.state.studentId, this.state.documentForm);
      }
      this.flashSuccess("Documento salvo.");
      this.resetDocumentForm();
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  async removeDocument(documentId: string): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.removeDocument(this.state.studentId, documentId);
      this.flashSuccess("Documento removido.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  setContactField = (field: keyof ContactInput, value: string | boolean) =>
    this.setState({ contactForm: { ...this.state.contactForm, [field]: value } as ContactInput });

  editContact = (contact: StudentContact) =>
    this.setState({
      editingContactId: contact.id,
      contactForm: {
        name: contact.name,
        relationship: contact.relationship ?? "",
        phone: contact.phone,
        email: contact.email ?? "",
        primary: contact.primary,
        notes: contact.notes ?? "",
      },
    });

  resetContactForm = () =>
    this.setState({ editingContactId: null, contactForm: { ...EMPTY_CONTACT_FORM } });

  async saveContact(): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      if (this.state.editingContactId) {
        await studentApi.updateContact(this.state.studentId, this.state.editingContactId, this.state.contactForm);
      } else {
        await studentApi.addContact(this.state.studentId, this.state.contactForm);
      }
      this.flashSuccess("Contato salvo.");
      this.resetContactForm();
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  async removeContact(contactId: string): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.removeContact(this.state.studentId, contactId);
      this.flashSuccess("Contato removido.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  setAddressField = (field: keyof AddressInput, value: string) =>
    this.setState({ addressForm: { ...this.state.addressForm, [field]: value } });

  async saveAddress(): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      const address = await studentApi.saveAddress(this.state.studentId, this.state.addressForm);
      this.setState({ address });
      this.flashSuccess("Endereço salvo.");
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  setAssignBranchId = (assignBranchId: string) => this.setState({ assignBranchId });
  setAssignPrimary = (assignPrimary: boolean) => this.setState({ assignPrimary });
  setTransferBranchId = (transferBranchId: string) => this.setState({ transferBranchId });
  setTransferReason = (transferReason: string) => this.setState({ transferReason });

  async assignBranch(): Promise<void> {
    if (!this.state.studentId || !this.state.assignBranchId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.assignBranch(this.state.studentId, this.state.assignBranchId, this.state.assignPrimary);
      this.setState({ assignBranchId: "", assignPrimary: false });
      this.flashSuccess("Unidade vinculada.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  async transfer(): Promise<void> {
    if (!this.state.studentId || !this.state.transferBranchId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.transfer(this.state.studentId, this.state.transferBranchId, this.state.transferReason);
      this.setState({ transferBranchId: "", transferReason: "" });
      this.flashSuccess("Transferência realizada.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  async removeBranch(assignmentId: string): Promise<void> {
    if (!this.state.studentId) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.removeBranch(this.state.studentId, assignmentId);
      this.flashSuccess("Vínculo encerrado.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  setNoteContent = (noteContent: string) => this.setState({ noteContent });

  async addNote(): Promise<void> {
    if (!this.state.studentId || !this.state.noteContent.trim()) return;
    this.setState({ saving: true, error: null });
    try {
      await studentApi.addNote(this.state.studentId, this.state.noteContent);
      this.setState({ noteContent: "" });
      this.flashSuccess("Observação registrada.");
      await this.refresh();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    } finally {
      this.setState({ saving: false });
    }
  }

  dismissFeedback = () => this.setState({ error: null, success: null });

  private flashSuccess(message: string) {
    this.setState({ success: message });
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.setState({ success: null }), 4000);
  }
}
