import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AuthInterceptor } from './auth/auth.interceptor';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { HomeComponent } from './pages/home/home.component';
import { ProfileSetupComponent } from './pages/setup/profile-setup.component';
import { ScheduleSetupComponent } from './pages/setup/schedule-setup.component';
import { UserProfileComponent } from './pages/profile/user-profile.component';
import { CounselorProfileComponent } from './pages/profile/counselor-profile.component';
import { CounselorListComponent } from './pages/counselors/counselor-list.component';
import { CounselorDetailComponent } from './pages/counselors/counselor-detail.component';
import { ClientAppointmentsComponent } from './pages/appointments/client-appointments.component';
import { CounselorSessionsComponent } from './pages/sessions/counselor-sessions.component';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    HomeComponent,
    ProfileSetupComponent,
    ScheduleSetupComponent,
    UserProfileComponent,
    CounselorProfileComponent,
    CounselorListComponent,
    CounselorDetailComponent,
    ClientAppointmentsComponent,
    CounselorSessionsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
