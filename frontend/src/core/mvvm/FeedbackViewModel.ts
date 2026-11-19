import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";

export interface FeedbackState {
  error: string | null;
  success: string | null;
  saving: boolean;
}

/**
 * ViewModel with the error/success/saving trio shared by the CRUD screens.
 * `run` wraps a mutation: it toggles `saving`, reports failures and flashes a
 * success message that clears itself.
 */
export abstract class FeedbackViewModel<S extends FeedbackState> extends ViewModel<S> {
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  dismissFeedback = () => this.setState({ error: null, success: null } as Partial<S>);

  protected async run(action: () => Promise<void>, successMessage?: string): Promise<boolean> {
    this.setState({ saving: true, error: null } as Partial<S>);
    try {
      await action();
      if (successMessage) this.flashSuccess(successMessage);
      return true;
    } catch (error) {
      this.setState({ error: errorMessage(error) } as Partial<S>);
      return false;
    } finally {
      this.setState({ saving: false } as Partial<S>);
    }
  }

  protected flashSuccess(message: string) {
    this.setState({ success: message } as Partial<S>);
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.setState({ success: null } as Partial<S>), 4000);
  }
}
