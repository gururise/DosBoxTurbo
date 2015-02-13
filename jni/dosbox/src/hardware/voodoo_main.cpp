#include <stdlib.h>
#include <string.h>

#include "dosbox.h"
#include "inout.h"
#include "mem.h"
#include "setup.h"
#include "debug.h"
#include "callback.h"
#include "regs.h"

#ifdef HAVE_NEON
#include "math_neon.h"
#endif

#include "pic.h"

#include "mem.h"
#include "paging.h"

#if defined(_MSC_VER) && (_MSC_VER >= 1400) 
#pragma warning(disable : 4224 4244 4018) 
#endif

#include "voodoo_types.h"
#include "voodoo_data.h"
voodoo_state *v;
#include "render.h"
#include "voodoo_main.h"
#include "voodoo_func.h"


Voodoo_PageHandler * voodoo_pagehandler;




class Voodoo_PageHandler : public PageHandler {
public:

	Voodoo_PageHandler(HostPt addr){
		flags=PFLAG_NOCODE;
	}

    ~Voodoo_PageHandler() {
    }

    Bitu readb(PhysPt addr) {
		return -1;
	}
	void writeb(PhysPt addr,Bitu val) {
	}

	void writew(PhysPt addr,Bitu val) {
		addr = PAGING_GetPhysicalAddress(addr);
		if (addr&3)
			voodoo_w((addr>>2)&0x3FFFFF,val<<16,0xffff0000);
		else
			voodoo_w((addr>>2)&0x3FFFFF,val,0x0000ffff);
	}

	Bitu readd(PhysPt addr) {
		addr = PAGING_GetPhysicalAddress(addr);
		return voodoo_r((addr>>2)&0x3FFFFF);
	}
    void writed(PhysPt addr,Bitu val) {
//		LOG_MSG("W %x v%x",addr,val);
		addr = PAGING_GetPhysicalAddress(addr);
		voodoo_w((addr>>2)&0x3FFFFF,val,0xffffffff);
    }

};

// Screen drawing
// fbiInit0 bit 0: passthrough control
extern void VGA_SetOverride(bool override);

typedef struct vdraw {
	Bitu width;
	Bitu height;
	Bitu bpp;
	float vfreq;
	double frame_start;
	bool doublewidth;
	bool doubleheight;

	bool clock_enabled;
	bool output_on;
	bool override_on;
	bool screen_update_pending;

} voodoo_draw;

static voodoo_draw vdraw;

static void Voodoo_VerticalTimer(Bitu /*val*/) {
	vdraw.frame_start = PIC_FullIndex();
	PIC_AddEvent( Voodoo_VerticalTimer, vdraw.vfreq );

	if (v->fbi.vblank_swap_pending==TRUE) {
		swap_buffers(v);
	}
	if (!RENDER_StartUpdate()) return; // frameskip

	rectangle r;
	r.min_x = r.min_y = 0;
	r.max_x = v->fbi.width;
	r.max_y = v->fbi.height;
	voodoo_update(&r);
	// draw all lines at once
	Bit16u *viewbuf = (Bit16u *)(v->fbi.ram + v->fbi.rgboffs[v->fbi.frontbuf]);
	for(Bitu i = 0; i < v->fbi.height; i++) {
		RENDER_DrawLine((Bit8u*) viewbuf);
		viewbuf += v->fbi.rowpixels;
	}
	RENDER_EndUpdate(false);
}

bool Voodoo_get_retrace() {
	// TODO proper implementation
	double time_in_frame = PIC_FullIndex() - vdraw.frame_start;
	if ((time_in_frame/vdraw.vfreq) > 0.95) return true;
	else return false;
}

void Voodoo_Output_Enable(bool enabled) {
	if (vdraw.output_on != enabled) {
		vdraw.output_on = enabled;
		Voodoo_UpdateScreenStart();
	}
}
void Voodoo_PCI_Enable(bool enable) {
	vdraw.clock_enabled=enable;
	Voodoo_UpdateScreenStart();
}

extern void VGA_SetOverride(bool override);

static void Voodoo_UpdateScreen(Bitu /*val*/) {
	vdraw.screen_update_pending = false;
	// abort drawing
	RENDER_EndUpdate(true);

	if ((!vdraw.clock_enabled || !vdraw.output_on)&& vdraw.override_on) {
		// switching off
		PIC_RemoveEvents(Voodoo_VerticalTimer);
		VGA_SetOverride(false);
		vdraw.override_on=false;
	}

	if ((vdraw.clock_enabled && vdraw.output_on)&& !vdraw.override_on) {
		// switching on
		PIC_RemoveEvents(Voodoo_VerticalTimer); // shouldn't be needed
		
		// TODO proper implementation of refresh rates and timings
		vdraw.vfreq = 1000.0/60.0;
		VGA_SetOverride(true);
		vdraw.override_on=true;

		vdraw.height=v->fbi.height;
		//LOG_MSG("Voodoo output %dx%d",v->fbi.width+1,v->fbi.height);
		
		RENDER_SetSize(v->fbi.width, v->fbi.height, 16, vdraw.vfreq, 4.0/3.0,
			false, false);
		Voodoo_VerticalTimer(0);
	}
}

void Voodoo_UpdateScreenStart() {
	if (!vdraw.screen_update_pending) {
		vdraw.screen_update_pending=true;
		PIC_AddEvent(Voodoo_UpdateScreen, 100.0);
	}
}

class VOODOO:public Module_base{
private:
	Bits emulation_type;
public:
	VOODOO(Section* configuration):Module_base(configuration){
		emulation_type=-1;

		Section_prop * section=static_cast<Section_prop *>(configuration);
		std::string voodoo_type_str(section->Get_string("voodoo"));
		if (voodoo_type_str=="false") {
			emulation_type=0;
		} else if (voodoo_type_str=="software") {
			emulation_type=1;
#if C_OPENGL
		} else if ((voodoo_type_str=="opengl") || (voodoo_type_str=="auto")) {
			emulation_type=2;
#else
		} else if (voodoo_type_str=="auto") {
			emulation_type=1;
#endif
		} else {
			emulation_type=0;
		}

		switch (emulation_type) {
			case 1:
			case 2:
				voodoo_pagehandler = new Voodoo_PageHandler(0);

				v = new voodoo_state;
				voodoo_init();
				break;
			default:
				break;
		}
	}

	~VOODOO(){
		if (emulation_type<=0) return;

		switch (emulation_type) {
			case 1:
			case 2:
				free(v->fbi.ram);
				free(v->tmu[0].ram);
				free(v->tmu[1].ram);
				delete v;
				delete voodoo_pagehandler;
				break;
			default:
				break;
		}

		emulation_type=-1;

	}
};

static VOODOO* voodoo_test;

void VOODOO_ShutDown(Section* sec){
	delete voodoo_test;
}

void VOODOO_Init(Section* sec) {
	voodoo_test = new VOODOO(sec);
	sec->AddDestroyFunction(&VOODOO_ShutDown,false);
}

#if defined(_MSC_VER) && (_MSC_VER >= 1400) 
#pragma warning(default : 4224 4244 4018) 
#endif
