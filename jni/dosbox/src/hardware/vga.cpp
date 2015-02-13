/*
 *  Copyright (C) 2002-2013  The DOSBox Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */


#include "dosbox.h"
//#include "setup.h"
#include "video.h"
#include "pic.h"
#include "vga.h"
#include "mem.h"

#include <string.h>

#ifdef HAVE_NEON
#include <arm_neon.h>
#endif


VGA_Type vga;
SVGA_Driver svga;

Bit32u CGA_2_Table[16];
Bit32u CGA_4_Table[256];
Bit32u CGA_4_HiRes_Table[256];
Bit32u CGA_16_Table[256];
Bit32u TXT_Font_Table[16];
Bit32u TXT_FG_Table[16];
Bit32u TXT_BG_Table[16];
Bit32u ExpandTable[256];
Bit32u Expand16Table[4][16];
Bit32u FillTable[16];
Bit32u ColorTable[16];

void VGA_SetModeNow(VGAModes mode) {
	if (vga.mode == mode) return;
	vga.mode=mode;
	VGA_SetupHandlers();
	VGA_StartResize(0);
}


void VGA_SetMode(VGAModes mode) {
	if (vga.mode == mode) return;
	vga.mode=mode;
	VGA_SetupHandlers();
	VGA_StartResize();
}

void VGA_DetermineMode(void) {
	if (svga.determine_mode) {
		svga.determine_mode();
		return;
	}
	/* Test for VGA output active or direct color modes */
	switch (vga.s3.misc_control_2 >> 4) {
	case 0:
		if (vga.attr.mode_control & 1) { // graphics mode
			if (IS_VGA_ARCH && (vga.gfx.mode & 0x40)) {
				// access above 256k?
				if (vga.s3.reg_31 & 0x8) VGA_SetMode(M_LIN8);
				else VGA_SetMode(M_VGA);
			}
			else if (vga.gfx.mode & 0x20) VGA_SetMode(M_CGA4);
			else if ((vga.gfx.miscellaneous & 0x0c)==0x0c) VGA_SetMode(M_CGA2);
			else {
				// access above 256k?
				if (vga.s3.reg_31 & 0x8) VGA_SetMode(M_LIN4);
				else VGA_SetMode(M_EGA);
			}
		} else {
			VGA_SetMode(M_TEXT);
		}
		break;
	case 1:VGA_SetMode(M_LIN8);break;
	case 3:VGA_SetMode(M_LIN15);break;
	case 5:VGA_SetMode(M_LIN16);break;
	case 13:VGA_SetMode(M_LIN32);break;
	}
}

void VGA_StartResize(Bitu delay /*=50*/) {
	if (!vga.draw.resizing) {
		vga.draw.resizing=true;
		if (vga.mode==M_ERROR) delay = 5;
		/* Start a resize after delay (default 50 ms) */
		if (delay==0) VGA_SetupDrawing(0);
		else PIC_AddEvent(VGA_SetupDrawing,(float)delay);
	}
}

void VGA_SetClock(Bitu which,Bitu target) {
	if (svga.set_clock) {
		svga.set_clock(which, target);
		return;
	}
	struct{
		Bitu n,m;
		Bits err;
	} best;
	best.err=target;
	best.m=1;
	best.n=1;
	Bitu n,r;
	Bits m;

	for (r = 0; r <= 3; r++) {
		Bitu f_vco = target * (1 << r);
		if (MIN_VCO <= f_vco && f_vco < MAX_VCO) break;
    }
	for (n=1;n<=31;n++) {
		m=(target * (n + 2) * (1 << r) + (S3_CLOCK_REF/2)) / S3_CLOCK_REF - 2;
		if (0 <= m && m <= 127)	{
			Bitu temp_target = S3_CLOCK(m,n,r);
			Bits err = target - temp_target;
			if (err < 0) err = -err;
			if (err < best.err) {
				best.err = err;
				best.m = m;
				best.n = n;
			}
		}
    }
	/* Program the s3 clock chip */
	vga.s3.clk[which].m=best.m;
	vga.s3.clk[which].r=r;
	vga.s3.clk[which].n=best.n;
	VGA_StartResize();
}

void VGA_SetCGA2Table(Bit8u val0,Bit8u val1) {

/*#ifdef HAVE_NEON
#ifdef WORDS_BIGENDIAN
	Bit8u total0[16]={val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1};
	Bit8u total1[16]={val0,val0,val1,val1,val0,val0,val1,val1,val0,val0,val1,val1,val0,val0,val1,val1};
	Bit8u total2[16]={val0,val0,val0,val0,val1,val1,val1,val1,val0,val0,val0,val0,val1,val1,val1,val1};
	Bit8u total3[16]={val0,val0,val0,val0,val0,val0,val0,val0,val1,val1,val1,val1,val1,val1,val1,val1};
#else
	Bit8u total0[16]={val0,val0,val0,val0,val0,val0,val0,val0,val1,val1,val1,val1,val1,val1,val1,val1};
	Bit8u total1[16]={val0,val0,val0,val0,val1,val1,val1,val1,val0,val0,val0,val0,val1,val1,val1,val1};
	Bit8u total2[16]={val0,val0,val1,val1,val0,val0,val1,val1,val0,val0,val1,val1,val0,val0,val1,val1};
	Bit8u total3[16]={val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1,val0,val1};
#endif
	uint8x16_t total0_t = vld1q_u8(total0);
	uint8x16_t total1_t = vqshlq_u8(vld1q_u8(total1),vdupq_n_s8(8));
	total0_t=vorrq_u8(total0_t,total1_t);
	uint8x16_t total2_t = vqshlq_u8(vld1q_u8(total2),vdupq_n_s8(16));
	total0_t=vorrq_u8(total0_t,total2_t);
	uint8x16_t total3_t = vqshlq_u8(vld1q_u8(total3),vdupq_n_s8(24));
	total0_t=vorrq_u8(total0_t,total3_t);

	
	CGA_2_Table[0] = vgetq_lane_u8(total0_t,0);
	CGA_2_Table[1] = vgetq_lane_u8(total0_t,1);
	CGA_2_Table[2] = vgetq_lane_u8(total0_t,2);
	CGA_2_Table[3] = vgetq_lane_u8(total0_t,3);
	CGA_2_Table[4] = vgetq_lane_u8(total0_t,4);
	CGA_2_Table[5] = vgetq_lane_u8(total0_t,5);
	CGA_2_Table[6] = vgetq_lane_u8(total0_t,6);
	CGA_2_Table[7] = vgetq_lane_u8(total0_t,7);
	CGA_2_Table[8] = vgetq_lane_u8(total0_t,8);
	CGA_2_Table[9] = vgetq_lane_u8(total0_t,9);
	CGA_2_Table[10] = vgetq_lane_u8(total0_t,10);
	CGA_2_Table[11] = vgetq_lane_u8(total0_t,11);
	CGA_2_Table[12] = vgetq_lane_u8(total0_t,12);
	CGA_2_Table[13] = vgetq_lane_u8(total0_t,13);
	CGA_2_Table[14] = vgetq_lane_u8(total0_t,14);
	CGA_2_Table[15] = vgetq_lane_u8(total0_t,15);
	
#else */
	Bit8u total[2]={ val0,val1};
	for (Bitu i=0;i<16;i++) {
		CGA_2_Table[i]=
#ifdef WORDS_BIGENDIAN
			(total[(i >> 0) & 1] << 0  ) | (total[(i >> 1) & 1] << 8  ) |
			(total[(i >> 2) & 1] << 16 ) | (total[(i >> 3) & 1] << 24 );
#else 
			(total[(i >> 3) & 1] << 0  ) | (total[(i >> 2) & 1] << 8  ) |
			(total[(i >> 1) & 1] << 16 ) | (total[(i >> 0) & 1] << 24 );
#endif
	}

//#endif
}

void VGA_SetCGA4Table(Bit8u val0,Bit8u val1,Bit8u val2,Bit8u val3) {


/*#ifdef HAVE_NEON
	Bit8u total[8]={ val0,val1,val2,val3,0,0,0,0};
	Bit8u total0[256];
	Bit8u total1[256];
	Bit8u total2[256];
	Bit8u total3[256];
	Bit8u mask = 3;
	for (Bitu i=0;i<256;i++)
	{
#ifdef WORDS_BIGENDIAN
		total0[i]= (i >> 0) & mask;
		total1[i]= (i >> 2) & mask;
		total2[i]= (i >> 4) & mask;
		total3[i]= (i >> 6) & mask;
#else
		total3[i]= (i >> 0) & mask;
		total2[i]= (i >> 2) & mask;
		total1[i]= (i >> 4) & mask;
		total0[i]= (i >> 6) & mask;
#endif
	}

	uint8x8_t total_t = vld1_u8(total);
	for (Bitu i=0;i<256;i+=8)
	{
		uint8x8_t total0_t = vtbl1_u8(total_t,vld1_u8(&total0[i]));
		vld1_lane_u8(&total0[i],total0_t,8);
		uint8x8_t total1_t = vtbl1_u8(total_t,vld1_u8(&total1[i]));
		vld1_lane_u8(&total1[i],total1_t,8);
		uint8x8_t total2_t = vtbl1_u8(total_t,vld1_u8(&total2[i]));
		vld1_lane_u8(&total2[i],total2_t,8);
		uint8x8_t total3_t = vtbl1_u8(total_t,vld1_u8(&total3[i]));
		vld1_lane_u8(&total3[i],total3_t,8);
	}
	for (Bitu i=0;i<256;i+=16) {
		uint8x16_t total0_t = vld1q_u8(&total0[i]);
		uint8x16_t total1_t = vqshlq_u8(vld1q_u8(&total1[i]),vdupq_n_s8(8));
		total0_t=vorrq_u8(total0_t,total1_t);
		uint8x16_t total2_t = vqshlq_u8(vld1q_u8(&total2[i]),vdupq_n_s8(16));
		total0_t=vorrq_u8(total0_t,total2_t);
		uint8x16_t total3_t = vqshlq_u8(vld1q_u8(&total3[i]),vdupq_n_s8(24));
		total0_t=vorrq_u8(total0_t,total3_t);
		
		CGA_4_Table[i+0] = vgetq_lane_u8(total0_t,0);
		CGA_4_Table[i+1] = vgetq_lane_u8(total0_t,1);
		CGA_4_Table[i+2] = vgetq_lane_u8(total0_t,2);
		CGA_4_Table[i+3] = vgetq_lane_u8(total0_t,3);
		CGA_4_Table[i+4] = vgetq_lane_u8(total0_t,4);
		CGA_4_Table[i+5] = vgetq_lane_u8(total0_t,5);
		CGA_4_Table[i+6] = vgetq_lane_u8(total0_t,6);
		CGA_4_Table[i+7] = vgetq_lane_u8(total0_t,7);
		CGA_4_Table[i+8] = vgetq_lane_u8(total0_t,8);
		CGA_4_Table[i+9] = vgetq_lane_u8(total0_t,9);
		CGA_4_Table[i+10] = vgetq_lane_u8(total0_t,10);
		CGA_4_Table[i+11] = vgetq_lane_u8(total0_t,11);
		CGA_4_Table[i+12] = vgetq_lane_u8(total0_t,12);
		CGA_4_Table[i+13] = vgetq_lane_u8(total0_t,13);
		CGA_4_Table[i+14] = vgetq_lane_u8(total0_t,14);
		CGA_4_Table[i+15] = vgetq_lane_u8(total0_t,15);
		
	}

	for (Bitu i=0;i<256;i++)
	{
#ifdef WORDS_BIGENDIAN
		total0[i]= ((i >> 0) & 1) | ((i >> 3) & 2);
		total1[i]= ((i >> 1) & 1) | ((i >> 4) & 2);
		total2[i]= ((i >> 2) & 1) | ((i >> 5) & 2);
		total3[i]= ((i >> 3) & 1) | ((i >> 6) & 2);
#else
		total3[i]= ((i >> 0) & 1) | ((i >> 3) & 2);
		total2[i]= ((i >> 1) & 1) | ((i >> 4) & 2);
		total1[i]= ((i >> 2) & 1) | ((i >> 5) & 2);
		total0[i]= ((i >> 3) & 1) | ((i >> 6) & 2);
#endif
	}

	for (Bitu i=0;i<256;i+=8)
	{
		uint8x8_t total0_t = vtbl1_u8(total_t,vld1_u8(&total0[i]));
		vld1_lane_u8(&total0[i],total0_t,8);
		uint8x8_t total1_t = vtbl1_u8(total_t,vld1_u8(&total1[i]));
		vld1_lane_u8(&total1[i],total1_t,8);
		uint8x8_t total2_t = vtbl1_u8(total_t,vld1_u8(&total2[i]));
		vld1_lane_u8(&total2[i],total2_t,8);
		uint8x8_t total3_t = vtbl1_u8(total_t,vld1_u8(&total3[i]));
		vld1_lane_u8(&total3[i],total3_t,8);
	}
	for (Bitu i=0;i<256;i+=16) {
		uint8x16_t total0_t = vld1q_u8(&total0[i]);
		uint8x16_t total1_t = vqshlq_u8(vld1q_u8(&total1[i]),vdupq_n_s8(8));
		total0_t=vorrq_u8(total0_t,total1_t);
		uint8x16_t total2_t = vqshlq_u8(vld1q_u8(&total2[i]),vdupq_n_s8(16));
		total0_t=vorrq_u8(total0_t,total2_t);
		uint8x16_t total3_t = vqshlq_u8(vld1q_u8(&total3[i]),vdupq_n_s8(24));
		total0_t=vorrq_u8(total0_t,total3_t);
		
		CGA_4_HiRes_Table[i+0] = vgetq_lane_u8(total0_t,0);
		CGA_4_HiRes_Table[i+1] = vgetq_lane_u8(total0_t,1);
		CGA_4_HiRes_Table[i+2] = vgetq_lane_u8(total0_t,2);
		CGA_4_HiRes_Table[i+3] = vgetq_lane_u8(total0_t,3);
		CGA_4_HiRes_Table[i+4] = vgetq_lane_u8(total0_t,4);
		CGA_4_HiRes_Table[i+5] = vgetq_lane_u8(total0_t,5);
		CGA_4_HiRes_Table[i+6] = vgetq_lane_u8(total0_t,6);
		CGA_4_HiRes_Table[i+7] = vgetq_lane_u8(total0_t,7);
		CGA_4_HiRes_Table[i+8] = vgetq_lane_u8(total0_t,8);
		CGA_4_HiRes_Table[i+9] = vgetq_lane_u8(total0_t,9);
		CGA_4_HiRes_Table[i+10] = vgetq_lane_u8(total0_t,10);
		CGA_4_HiRes_Table[i+11] = vgetq_lane_u8(total0_t,11);
		CGA_4_HiRes_Table[i+12] = vgetq_lane_u8(total0_t,12);
		CGA_4_HiRes_Table[i+13] = vgetq_lane_u8(total0_t,13);
		CGA_4_HiRes_Table[i+14] = vgetq_lane_u8(total0_t,14);
		CGA_4_HiRes_Table[i+15] = vgetq_lane_u8(total0_t,15);
	}

#else  */

	Bit8u total[4]={ val0,val1,val2,val3};
	for (Bitu i=0;i<256;i++) {
		CGA_4_Table[i]=
#ifdef WORDS_BIGENDIAN
			(total[(i >> 0) & 3] << 0  ) | (total[(i >> 2) & 3] << 8  ) |
			(total[(i >> 4) & 3] << 16 ) | (total[(i >> 6) & 3] << 24 );
#else
			(total[(i >> 6) & 3] << 0  ) | (total[(i >> 4) & 3] << 8  ) |
			(total[(i >> 2) & 3] << 16 ) | (total[(i >> 0) & 3] << 24 );
#endif
		CGA_4_HiRes_Table[i]=
#ifdef WORDS_BIGENDIAN
			(total[((i >> 0) & 1) | ((i >> 3) & 2)] << 0  ) | (total[((i >> 1) & 1) | ((i >> 4) & 2)] << 8  ) |
			(total[((i >> 2) & 1) | ((i >> 5) & 2)] << 16 ) | (total[((i >> 3) & 1) | ((i >> 6) & 2)] << 24 );
#else
			(total[((i >> 3) & 1) | ((i >> 6) & 2)] << 0  ) | (total[((i >> 2) & 1) | ((i >> 5) & 2)] << 8  ) |
			(total[((i >> 1) & 1) | ((i >> 4) & 2)] << 16 ) | (total[((i >> 0) & 1) | ((i >> 3) & 2)] << 24 );
#endif
	}
//#endif
}

void VGA_Init(Section* sec) {
//	Section_prop * section=static_cast<Section_prop *>(sec);
	vga.draw.resizing=false;
	vga.mode=M_ERROR;			//For first init
	SVGA_Setup_Driver();
	VGA_SetupMemory(sec);
	VGA_SetupMisc();
	VGA_SetupDAC();
	VGA_SetupGFX();
	VGA_SetupSEQ();
	VGA_SetupAttr();
	VGA_SetupOther();
	VGA_SetupXGA();
	VGA_SetClock(0,CLK_25);
	VGA_SetClock(1,CLK_28);
/* Generate tables */
	VGA_SetCGA2Table(0,1);
	VGA_SetCGA4Table(0,1,2,3);
	Bitu i,j;
	for (i=0;i<256;i++) {
		ExpandTable[i]=i | (i << 8)| (i <<16) | (i << 24);
	}
	for (i=0;i<16;i++) {
		TXT_FG_Table[i]=i | (i << 8)| (i <<16) | (i << 24);
		TXT_BG_Table[i]=i | (i << 8)| (i <<16) | (i << 24);
#ifdef WORDS_BIGENDIAN
		FillTable[i]=
			((i & 1) ? 0xff000000 : 0) |
			((i & 2) ? 0x00ff0000 : 0) |
			((i & 4) ? 0x0000ff00 : 0) |
			((i & 8) ? 0x000000ff : 0) ;
		TXT_Font_Table[i]=
			((i & 1) ? 0x000000ff : 0) |
			((i & 2) ? 0x0000ff00 : 0) |
			((i & 4) ? 0x00ff0000 : 0) |
			((i & 8) ? 0xff000000 : 0) ;
#else 
		FillTable[i]=
			((i & 1) ? 0x000000ff : 0) |
			((i & 2) ? 0x0000ff00 : 0) |
			((i & 4) ? 0x00ff0000 : 0) |
			((i & 8) ? 0xff000000 : 0) ;
		TXT_Font_Table[i]=	
			((i & 1) ? 0xff000000 : 0) |
			((i & 2) ? 0x00ff0000 : 0) |
			((i & 4) ? 0x0000ff00 : 0) |
			((i & 8) ? 0x000000ff : 0) ;
#endif
	}
	for (j=0;j<4;j++) {
		for (i=0;i<16;i++) {
#ifdef WORDS_BIGENDIAN
			Expand16Table[j][i] =
				((i & 1) ? 1 << j : 0) |
				((i & 2) ? 1 << (8 + j) : 0) |
				((i & 4) ? 1 << (16 + j) : 0) |
				((i & 8) ? 1 << (24 + j) : 0);
#else
			Expand16Table[j][i] =
				((i & 1) ? 1 << (24 + j) : 0) |
				((i & 2) ? 1 << (16 + j) : 0) |
				((i & 4) ? 1 << (8 + j) : 0) |
				((i & 8) ? 1 << j : 0);
#endif
		}
	}
}

void SVGA_Setup_Driver(void) {
#if NEON_MEMORY
		memset_neon(&svga, 0, sizeof(SVGA_Driver));
#else
		memset(&svga, 0, sizeof(SVGA_Driver));
#endif

	switch(svgaCard) {
	case SVGA_S3Trio:
		SVGA_Setup_S3Trio();
		break;
	case SVGA_TsengET4K:
		SVGA_Setup_TsengET4K();
		break;
	case SVGA_TsengET3K:
		SVGA_Setup_TsengET3K();
		break;
	case SVGA_ParadisePVGA1A:
		SVGA_Setup_ParadisePVGA1A();
		break;
	default:
		vga.vmemsize = vga.vmemwrap = 256*1024;
		break;
	}
}
