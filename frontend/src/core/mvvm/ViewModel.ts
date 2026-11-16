/**
 * Minimal observable ViewModel base class for the MVVM layer. Pages subscribe
 * through useViewModel(); the ViewModel owns state transitions and calls into
 * repositories — views never talk to the API directly.
 */
export abstract class ViewModel<S extends object> {
  private _state: S;
  private readonly listeners = new Set<() => void>();

  protected constructor(initialState: S) {
    this._state = initialState;
  }

  getSnapshot = (): S => this._state;

  protected get state(): S {
    return this._state;
  }

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  protected setState(patch: Partial<S>): void {
    this._state = { ...this._state, ...patch };
    this.listeners.forEach((listener) => listener());
  }
}
