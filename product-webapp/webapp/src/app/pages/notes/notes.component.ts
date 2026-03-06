import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';
import { AuthStateService } from '../../core/auth-state.service';
import { AppointmentService, CounselorAppointment } from '../../services/appointment.service';
import { CounselorProfile, CounselorService } from '../../services/counselor.service';
import { SessionNote, SessionNoteService } from '../../services/session-note.service';

@Component({
  selector: 'app-notes',
  templateUrl: './notes.component.html',
  styleUrls: ['./notes.component.css']
})
export class NotesComponent implements OnInit {
  role: 'CLIENT' | 'THERAPIST' | null = null;
  counselorProfile: CounselorProfile | null = null;
  sessions: CounselorAppointment[] = [];
  notes: SessionNote[] = [];
  notesByAppointment = new Map<number, SessionNote>();
  draftByAppointment = new Map<number, boolean>();

  selectedAppointment: CounselorAppointment | null = null;
  selectedNote: SessionNote | null = null;

  loading = false;
  saving = false;
  message = '';
  error = '';
  pdfPreviewUrl: SafeResourceUrl | null = null;
  pdfPreviewTitle = '';

  form = {
    summary: '',
    observations: '',
    recommendations: '',
    privateNotes: ''
  };

  constructor(
    private authState: AuthStateService,
    private counselorService: CounselorService,
    private appointmentService: AppointmentService,
    private sessionNoteService: SessionNoteService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const user = this.authState.currentUser;
    if (!user) {
      return;
    }
    this.role = user.role;
    if (user.role === 'THERAPIST') {
      this.loadCounselorData();
    } else {
      this.loadClientNotes(user.userId);
    }
  }

  private loadCounselorData(): void {
    this.loading = true;
    this.message = '';
    this.error = '';
    this.counselorService.getMe().subscribe({
      next: profile => {
        this.counselorProfile = profile;
        forkJoin({
          sessions: this.appointmentService.listForCounselor(profile.id),
          notes: this.sessionNoteService.getNotesForCounselor(profile.id)
        }).subscribe({
          next: ({ sessions, notes }) => {
            this.sessions = sessions || [];
            this.setNotes(notes || []);
            this.loading = false;
          },
          error: () => {
            this.error = 'Unable to load sessions or notes.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Unable to load counselor profile.';
        this.loading = false;
      }
    });
  }

  private loadClientNotes(userId: number): void {
    this.loading = true;
    this.sessionNoteService.getNotesForUser(userId).subscribe({
      next: notes => {
        this.notes = notes || [];
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load notes.';
        this.loading = false;
      }
    });
  }

  selectAppointment(appointment: CounselorAppointment): void {
    this.selectedAppointment = appointment;
    this.selectedNote = this.notesByAppointment.get(appointment.appointmentId) || null;
    const draft = this.loadDraft(appointment.appointmentId);
    if (draft) {
      this.form.summary = draft.summary || '';
      this.form.observations = draft.observations || '';
      this.form.recommendations = draft.recommendations || '';
      this.form.privateNotes = draft.privateNotes || '';
    } else {
      this.form.summary = '';
      this.form.observations = '';
      this.form.recommendations = '';
      this.form.privateNotes = '';
    }
    this.message = '';
    this.error = '';
  }

  saveNote(): void {
    if (!this.selectedAppointment) {
      return;
    }
    this.saving = true;
    this.message = '';
    this.error = '';
    const payload = {
      summary: this.form.summary.trim(),
      observations: this.form.observations.trim(),
      recommendations: this.form.recommendations.trim(),
      privateNotes: this.form.privateNotes.trim()
    };

    if (!payload.summary || !payload.observations || !payload.recommendations) {
      this.saving = false;
      this.error = 'Summary, observations, and recommendations are required.';
      return;
    }

    this.saveDraft(this.selectedAppointment.appointmentId, payload);
    this.saving = false;
    this.message = 'Draft saved locally.';
  }

  shareNote(): void {
    if (!this.selectedAppointment) {
      return;
    }
    this.saving = true;
    this.message = '';
    this.error = '';
    const payload = this.loadDraft(this.selectedAppointment.appointmentId);
    if (!payload || !payload.summary || !payload.observations || !payload.recommendations) {
      this.saving = false;
      this.error = 'Please save a complete draft before sharing.';
      return;
    }
    this.sessionNoteService.shareWithContent({
      appointmentId: this.selectedAppointment.appointmentId,
      summary: payload.summary,
      observations: payload.observations,
      recommendations: payload.recommendations,
      privateNotes: payload.privateNotes
    }).subscribe({
      next: note => {
        this.saving = false;
        this.message = 'Notes shared with client.';
        this.upsertNote(note);
        this.selectedNote = note;
        this.clearDraft(this.selectedAppointment?.appointmentId);
      },
      error: err => {
        this.saving = false;
        this.error = err?.error?.error || 'Unable to share notes.';
      }
    });
  }

  openPdf(url: string, title: string): void {
    if (!url) {
      return;
    }
    this.pdfPreviewTitle = title;
    this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  closePdf(): void {
    this.pdfPreviewUrl = null;
    this.pdfPreviewTitle = '';
  }

  canCreate(appointment: CounselorAppointment): boolean {
    if (this.selectedNote?.sharedWithClient) {
      return false;
    }
    return this.nowAfter(appointment.appointmentDate, appointment.startTime);
  }

  canShare(appointment: CounselorAppointment): boolean {
    if (!appointment?.appointmentDate || !appointment?.endTime) {
      return false;
    }
    const end = new Date(`${appointment.appointmentDate}T${appointment.endTime}`);
    const windowStart = new Date(end.getTime() - 15 * 60 * 1000);
    const windowEnd = new Date(end.getTime() + 15 * 60 * 1000);
    const now = new Date();
    return now >= windowStart && now <= windowEnd;
  }

  hasSessionEnded(appointment: CounselorAppointment): boolean {
    if (!appointment?.appointmentDate || !appointment?.endTime) {
      return false;
    }
    const end = new Date(`${appointment.appointmentDate}T${appointment.endTime}`);
    return new Date().getTime() > end.getTime();
  }

  formatSession(appointment: CounselorAppointment): string {
    return `${appointment.appointmentDate} • ${this.formatTime(appointment.startTime)} - ${this.formatTime(appointment.endTime)}`;
  }

  isSelected(appointment: CounselorAppointment): boolean {
    return this.selectedAppointment?.appointmentId === appointment.appointmentId;
  }

  hasDraft(appointment: CounselorAppointment): boolean {
    return this.draftByAppointment.get(appointment.appointmentId) === true;
  }

  private setNotes(notes: SessionNote[]): void {
    this.notes = notes;
    this.notesByAppointment = new Map(notes.map(note => [note.appointmentId, note]));
  }

  private upsertNote(note: SessionNote): void {
    const existingIndex = this.notes.findIndex(item => item.id === note.id);
    if (existingIndex >= 0) {
      this.notes[existingIndex] = note;
    } else {
      this.notes = [note, ...this.notes];
    }
    this.notesByAppointment.set(note.appointmentId, note);
  }

  private draftKey(appointmentId: number): string {
    return `session-note-draft-${appointmentId}`;
  }

  private loadDraft(appointmentId: number): any | null {
    try {
      const raw = localStorage.getItem(this.draftKey(appointmentId));
      if (!raw) {
        this.draftByAppointment.set(appointmentId, false);
        return null;
      }
      const parsed = JSON.parse(raw);
      this.draftByAppointment.set(appointmentId, true);
      return parsed;
    } catch {
      this.draftByAppointment.set(appointmentId, false);
      return null;
    }
  }

  private saveDraft(appointmentId: number, payload: any): void {
    localStorage.setItem(this.draftKey(appointmentId), JSON.stringify(payload));
    this.draftByAppointment.set(appointmentId, true);
  }

  private clearDraft(appointmentId?: number | null): void {
    if (!appointmentId) {
      return;
    }
    localStorage.removeItem(this.draftKey(appointmentId));
    this.draftByAppointment.set(appointmentId, false);
  }

  private nowAfter(date: string, time: string): boolean {
    if (!date || !time) {
      return false;
    }
    const start = new Date(`${date}T${time}`);
    return new Date().getTime() >= start.getTime();
  }

  isShared(): boolean {
    return !!this.selectedNote?.sharedWithClient;
  }

  private formatTime(value?: string): string {
    if (!value) {
      return '';
    }
    return value.slice(0, 5);
  }
}
